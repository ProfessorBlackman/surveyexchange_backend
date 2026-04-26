## 1. Mission

You are building the backend for **SurveyExchange**, a credit-based survey exchange platform.

This system includes a **financial-like credit economy**.

Your primary responsibility is:
- Data integrity
- Correctness of transactions
- Enforcing constraints at the database level

If anything conflicts:
> Correctness > Performance > Convenience

---

## 2. Tech Stack (MANDATORY)

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL
- Redis (caching only)

DO NOT:
- Use Node.js
- Use NoSQL databases for core data
- Use manual JDBC queries for core logic

---

## 3. Architecture Rules (STRICT)

### 3.1 Layered Architecture

```

Controller → Service → Repository

```

Rules:
- Controllers: request/response only
- Services: ALL business logic
- Repositories: DB access only

DO NOT:
- Put business logic in controllers
- Call repositories directly from controllers

---

### 3.2 Package Structure

```

com.surveyexchange  
/controller  
/service  
/repository  
/model  
/dto  
/security  
/config

````

---

## 4. Core Domain Models

You MUST implement:

- User
- Survey
- Question
- Response
- Answer
- CreditTransaction

---

## 5. CRITICAL: Credit System Rules

This is the most important part of the system.

### 5.1 Credit Actions

| Action | Credits |
|--------|--------|
| Signup | +10 |
| Survey response | +1 |
| Boost survey | -5 |

---

### 5.2 NON-NEGOTIABLE RULES

1. ALL credit changes must be recorded
2. Credit transactions are APPEND-ONLY (never update/delete)
3. Credits must NEVER become inconsistent
4. No partial updates allowed

---

### 5.3 Atomic Transactions (MANDATORY)

Use:
```java
@Transactional
````

Example flow (response submission):

1. Validate user has not responded
2. Save response
3. Increment user credits
4. Insert credit transaction log

If ANY step fails:  
→ ROLLBACK EVERYTHING

---

### 5.4 NEVER DO THIS

- Update credits without transaction
- Save response without updating credits
- Update credits outside service layer

---

## 6. Database Constraints (MANDATORY)

### 6.1 Unique Constraint

Prevent duplicate responses:

```sql
UNIQUE(user_id, survey_id)
```

This MUST exist at database level.

---

### 6.2 Credit Transactions

- Append-only table
- No UPDATE
- No DELETE

---

### 6.3 Indexes

Required:

- survey(boostExpiry)
- response(user_id, survey_id)

---

## 7. Authentication & Security

### 7.1 Authentication

- JWT-based
- Token expiry: 24

---

### 7.2 Passwords

- MUST use
- NEVER store plain

---

### 7.3 Security Config

- Stateless sessions
- Protected endpoints
- CORS restricted to frontend

---

## 8. Survey System

### 8.1 Survey Creation

Fields:

- title
- description
- category
- estimatedTime
- targetResponses

Questions:

- multiple choice
- checkbox
- short text
- rating (1–5)
- yes/no

---

### 8.2 Survey Feed

Requirements:

- Exclude:
    - user's own surveys
    - already answered surveys
- Order:
    - boosted first
    - then newest

Implementation MUST be at DB level:

```sql
ORDER BY 
  CASE WHEN boost_expiry > NOW() THEN 1 ELSE 0 END DESC,
  created_at DESC
```

DO NOT:

- Sort in memory

---

## 9. Response System

### Rules

- One response per user per survey
- Cannot respond to own survey

Enforcement:

- Application-level validation
- AND database constraint

---

## 10. Boosting System

### Rules

- Cost: 5 credits
- Duration: 24 hours

Flow:

1. Check user balance
2. Deduct credits
3. Set boostExpiry

Must be transactional.

---

## 11. Redis Usage (LIMITED)

Use Redis ONLY for:

- Feed caching
- Survey detail caching

DO NOT:

- Store critical data in Redis
- Use Redis as source of truth

---

## 12. API Design Rules

Base path:

```
/api
```

### Required Endpoints

Auth:

- POST `/auth/register`
- POST `/auth/login`

Surveys:

- GET `/surveys/feed`
- GET `/surveys/mine`
- POST `/surveys`
- GET `/surveys/{id}`
- POST `/surveys/{id}/respond`
- POST `/surveys/{id}/boost`

Credits:

- GET `/surveys/credits/history`

---

## 13. Performance Rules

- Feed query must be optimized
- No N+1 queries
- Use JOIN FETCH where necessary

Target:

- <500ms response time

---

## 14. Error Handling

Return proper HTTP codes:

- 400 → validation error
- 401 → unauthorized
- 403 → forbidden
- 404 → not found

Responses must include:

- message
- error code (optional)

---

## 15. Anti-Patterns (FORBIDDEN)

DO NOT:

- Skip database constraints
- Use in-memory checks for critical rules
- Break transactional boundaries
- Store business logic in controllers
- Return entities directly (use DTOs)

---

## 16. Definition of Done

A feature is complete only if:

- Business logic is correct
- DB constraints exist
- Transactions are safe
- API responses are correct
- Edge cases handled

---

## 17. Development Order (MANDATORY)

1. Project setup
2. Security (JWT + auth)
3. User entity + registration
4. Survey creation
5. Survey feed
6. Response system (WITH transactions)
7. Credit system
8. Boosting
9. Credit history
10. Redis caching (last)

DO NOT skip order.

---

## 18. Guiding Principle

> This backend behaves like a financial system.

Credits = money.

If your logic allows:

- double credits
- duplicate responses
- inconsistent balances

Then the system is broken.

---

## 19. Final Warning

The most common failure modes are:

- Missing @Transactional
- Missing DB constraints
- Race conditions on credit updates

You MUST prevent all three.

If unsure:

- Choose safety over optimization
- Choose correctness over speed