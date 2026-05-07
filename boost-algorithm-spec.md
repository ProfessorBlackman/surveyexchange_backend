# Survey Boost Algorithm — Technical Specification

**Platform:** SurveyExchange  
**Backend:** Java Spring Boot  
**Frontend:** React  
**Version:** 1.0 — MVP  

---

## 1. Overview

The boost algorithm allows a user to pay 5 credits to promote one of their own surveys to the top of the feed for 24 hours. Reliability is defined across three axes:

- **Atomicity** — credit deduction, log write, and boost activation either all succeed or all roll back. No partial state.
- **Idempotency** — a survey with an active boost cannot be double-boosted. The system rejects the request cleanly rather than silently double-charging.
- **Passive expiry** — boosts expire by timestamp comparison at query time. No background job or scheduler is required.

---

## 2. Constants

| Constant | Value | Description |
|---|---|---|
| `BOOST_COST` | `5` | Credits deducted per boost |
| `BOOST_DURATION_HOURS` | `24` | How long a boost stays active |
| `BOOST_MIN_BALANCE` | `5` | Minimum balance required to boost |

Define these in a central `BoostConstants.java` or in `application.properties` — never hardcoded inline.

---

## 3. API Contract

### Request

```
POST /api/surveys/{surveyId}/boost
Authorization: Bearer <jwt_token>
```

No request body required.

### Responses

| Status | Condition | Body |
|---|---|---|
| `200 OK` | Boost applied successfully | `{ "newBalance": int, "boostExpiresAt": ISO-8601 }` |
| `401 Unauthorized` | Missing or invalid JWT | `{ "error": "Unauthorized" }` |
| `403 Forbidden` | Requester does not own the survey | `{ "error": "You do not own this survey" }` |
| `402 Payment Required` | Credit balance below 5 | `{ "error": "Insufficient credits", "balance": int }` |
| `409 Conflict` | Survey already has an active boost | `{ "error": "Survey is already boosted", "boostExpiresAt": ISO-8601 }` |
| `404 Not Found` | Survey ID does not exist | `{ "error": "Survey not found" }` |
| `500 Internal Server Error` | Transaction failure or unexpected error | `{ "error": "Boost failed. Please try again." }` |

---

## 4. Algorithm — Step by Step

### Phase 1: Pre-flight Guards

These run before acquiring any database locks. They short-circuit early for common failure cases without touching a transaction.

#### Guard 1 — Authentication
The JWT filter (Spring Security) handles this before the controller is reached. No explicit check needed in service logic.

#### Guard 2 — Survey Exists
```java
Survey survey = surveyRepository.findById(surveyId)
    .orElseThrow(() -> new SurveyNotFoundException(surveyId));
```

#### Guard 3 — Ownership
```java
if (!survey.getOwner().getId().equals(currentUser.getId())) {
    throw new ForbiddenException("You do not own this survey");
}
```

#### Guard 4 — Active Boost Check (Idempotency)
Use a timestamp comparison, not a boolean flag. This ensures expiry is passive and requires no reset job.

```java
if (survey.getBoostExpiresAt() != null &&
    survey.getBoostExpiresAt().isAfter(Instant.now())) {
    throw new AlreadyBoostedException(survey.getBoostExpiresAt());
}
```

#### Guard 5 — Credit Balance (Pre-check)
Check before entering the transaction to avoid acquiring locks for the common rejection case.

```java
if (currentUser.getCreditBalance() < BOOST_COST) {
    throw new InsufficientCreditsException(currentUser.getCreditBalance());
}
```

---

### Phase 2: Atomic Transaction

All three mutations happen inside a single `@Transactional` block. If any step fails, Spring rolls back the entire unit — no partial state is persisted.

```java
@Transactional
public BoostResult applyBoost(Long userId, Long surveyId) {

    // Re-acquire user row with a pessimistic write lock.
    // Prevents race condition where two concurrent requests
    // both pass the pre-flight balance check.
    User user = userRepository.findByIdWithLock(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    Survey survey = surveyRepository.findById(surveyId)
        .orElseThrow(() -> new SurveyNotFoundException(surveyId));

    // Re-verify balance inside the lock (double-checked locking pattern)
    if (user.getCreditBalance() < BOOST_COST) {
        throw new InsufficientCreditsException(user.getCreditBalance());
    }

    // Step A: Deduct credits
    user.setCreditBalance(user.getCreditBalance() - BOOST_COST);
    userRepository.save(user);

    // Step B: Write append-only credit log entry
    CreditLog log = CreditLog.builder()
        .user(user)
        .amount(-BOOST_COST)
        .reason(CreditLogReason.BOOST)
        .survey(survey)
        .createdAt(Instant.now())
        .build();
    creditLogRepository.save(log);

    // Step C: Set boost expiry (not a boolean — a timestamp)
    Instant expiresAt = Instant.now().plus(BOOST_DURATION_HOURS, ChronoUnit.HOURS);
    survey.setBoostExpiresAt(expiresAt);
    surveyRepository.save(survey);

    return new BoostResult(user.getCreditBalance(), expiresAt);
}
```

#### Repository: Pessimistic Lock Query

```java
// SurveyRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdWithLock(@Param("id") Long id);
```

> **Why pessimistic locking?**  
> Without it, two concurrent boost requests can both read `balance = 6`, both pass the pre-flight check, and both deduct — leaving the user at `-4`. The `SELECT ... FOR UPDATE` lock serialises concurrent access to the same user row.

---

### Phase 3: Feed Ordering (No Background Job)

The boost does not require a scheduled task to expire. The feed query handles ordering at the database level using the timestamp:

```java
// JPQL in SurveyRepository
@Query("""
    SELECT s FROM Survey s
    WHERE s.owner.id != :userId
    AND s.id NOT IN (
        SELECT r.survey.id FROM Response r WHERE r.respondent.id = :userId
    )
    ORDER BY
        CASE WHEN s.boostExpiresAt > CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
        s.createdAt DESC
    """)
Page<Survey> findFeedForUser(@Param("userId") Long userId, Pageable pageable);
```

When `boost_expires_at` lapses, the survey automatically drops to the non-boosted tier on the next feed request. No `UPDATE` query, no cron, no event.

---

## 5. Data Model Changes

### `surveys` table — one column addition

```sql
ALTER TABLE surveys
ADD COLUMN boost_expires_at TIMESTAMPTZ NULL;
```

`NULL` means never boosted or boost has been cleared. The feed query handles both `NULL` and expired timestamps correctly via `CURRENT_TIMESTAMP` comparison.

### `credit_logs` table — no changes needed

The existing append-only credit log handles boost transactions with `reason = 'BOOST'` and `amount = -5`. No schema change required.

---

## 6. Java Layer Responsibilities

Following the layered architecture requirement (`controller → service → repository`):

| Layer | Responsibility |
|---|---|
| `BoostController` | Parse path variable, resolve current user from JWT, call service, map result to response DTO |
| `BoostService` | Run pre-flight guards, call `@Transactional` method, throw typed exceptions |
| `SurveyRepository` | Feed query with DB-level ordering; pessimistic lock query |
| `UserRepository` | Pessimistic write lock on user row |
| `CreditLogRepository` | Append-only insert; never exposes update or delete |

### Controller sketch

```java
@RestController
@RequestMapping("/api/surveys")
public class BoostController {

    private final BoostService boostService;

    @PostMapping("/{surveyId}/boost")
    public ResponseEntity<BoostResponseDto> boost(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        BoostResult result = boostService.boost(userDetails.getUserId(), surveyId);
        return ResponseEntity.ok(BoostResponseDto.from(result));
    }
}
```

---

## 7. React Frontend Behaviour

### Immediate balance update

Per the non-functional requirement, the credit balance must update without a page reload after a successful boost.

```js
// Using React Query (recommended pattern for this codebase)
const { mutate: boostSurvey, isLoading } = useMutation({
  mutationFn: (surveyId) =>
    api.post(`/surveys/${surveyId}/boost`),

  onSuccess: (data) => {
    // Immediately update cached balance — no refetch needed
    queryClient.setQueryData(['user', 'balance'], data.newBalance);

    // Invalidate feed so boosted survey rises to top
    queryClient.invalidateQueries({ queryKey: ['feed'] });

    // Invalidate user's own survey list (shows boost badge + expiry)
    queryClient.invalidateQueries({ queryKey: ['my-surveys'] });
  },

  onError: (error) => {
    const status = error.response?.status;
    if (status === 402) showToast('Not enough credits to boost');
    if (status === 409) showToast('This survey is already boosted');
    if (status === 403) showToast('You can only boost your own surveys');
  }
});
```

### Boost button state

The boost button should reflect three states:

| State | Condition | UI |
|---|---|---|
| Available | `boostExpiresAt` is null or past | "Boost for 5 credits" — enabled |
| Active | `boostExpiresAt` is in the future | "Boosted — expires in Xh" — disabled |
| Insufficient | `user.balance < 5` | "Boost for 5 credits" — disabled + tooltip |

---

## 8. Error Handling

### Custom exceptions (Spring)

```java
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException { ... }

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)   // 402
public class InsufficientCreditsException extends RuntimeException {
    private final int currentBalance;
    ...
}

@ResponseStatus(HttpStatus.CONFLICT)           // 409
public class AlreadyBoostedException extends RuntimeException {
    private final Instant expiresAt;
    ...
}
```

Handle all of these in a `@RestControllerAdvice` global exception handler that formats consistent JSON error bodies.

---

## 9. Edge Cases

| Scenario | Handling |
|---|---|
| Two concurrent boost requests for the same user | Pessimistic lock on user row serialises them; second request fails inside the transaction with `InsufficientCreditsException` if first drained the balance |
| Boost request while balance is exactly 5 | Passes — `>= 5` check; balance drops to 0 post-boost |
| Survey deleted while boost is active | Cascade delete removes survey; credit is not refunded (MVP scope) |
| Clock skew between app servers | Use `CURRENT_TIMESTAMP` in SQL for expiry checks — database clock is the source of truth, not `Instant.now()` on the app server |
| Re-boost after expiry | Allowed — guard 4 only blocks if `boostExpiresAt > NOW()`. Expired surveys are treated as never-boosted |

---

## 10. What This Spec Does Not Cover (Future Scope)

- **Paid boosts** via mobile money (Phase 2 monetisation)
- **Re-boost / extend** — extending an active boost by 24 hours from now rather than rejecting
- **Fraud detection** — detecting users who boost low-effort surveys to farm feed position
- **Boost analytics** — tracking whether boosted surveys convert to more responses

---

*Spec authored for SurveyExchange MVP — internal reference document.*
