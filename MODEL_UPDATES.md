# Data Collection Specification

**Platform:** SurveyExchange  
**Backend:** Java Spring Boot (JPA/Hibernate)  
**Version:** 1.0 — MVP + Analytics Foundation  

---

## 1. Overview

This document specifies all data fields to collect across user registration, survey creation, survey responses, and platform analytics. Fields are grouped by when they are collected and annotated with their purpose, whether they are MVP-required or deferred, and any schema implications.

### Priority legend

| Tag           | Meaning                                       |
|---------------|-----------------------------------------------|
| `[MVP]`       | Collect now — required for core functionality |
| `[ANALYTICS]` | Collect now — low effort, high future value   |
| `[FUTURE]`    | Defer — needed for a specific later feature   |

---

## 2. User Registration

### 2.1 Schema — `users` table

| Field             | Type                  | Tag           | Notes                                |
|-------------------|-----------------------|---------------|--------------------------------------|
| `id`              | `BIGINT` PK           | `[MVP]`       | Auto-generated                       |
| `email`           | `VARCHAR(255)` UNIQUE | `[MVP]`       | Lowercased on write                  |
| `password_hash`   | `VARCHAR(255)`        | `[MVP]`       | BCrypt — never plain text            |
| `full_name`       | `VARCHAR(255)`        | `[MVP]`       | Display name                         |
| `institution`     | `VARCHAR(255)`        | `[MVP]`       | University or research body          |
| `field_of_study`  | `VARCHAR(255)`        | `[MVP]`       | e.g. "Computer Science"              |
| `academic_level`  | `ENUM`                | `[MVP]`       | See values below                     |
| `year_of_study`   | `SMALLINT` NULL       | `[ANALYTICS]` | 1–8; null for researchers/lecturers  |
| `gender`          | `ENUM` NULL           | `[ANALYTICS]` | See values below; optional at signup |
| `age_range`       | `ENUM` NULL           | `[ANALYTICS]` | See values below; optional at signup |
| `credit_balance`  | `INT`                 | `[MVP]`       | Default 10 (signup bonus)            |
| `referral_source` | `VARCHAR(100)` NULL   | `[ANALYTICS]` | How they found the platform          |
| `created_at`      | `TIMESTAMPTZ`         | `[MVP]`       | Set on insert, never updated         |
| `last_active_at`  | `TIMESTAMPTZ`         | `[ANALYTICS]` | Updated on any authenticated request |
| `version`         | `BIGINT`              | `[MVP]`       | Optimistic locking (`@Version`)      |

### 2.2 Enum values

**`academic_level`**
```
UNDERGRADUATE, POSTGRADUATE_MASTERS, POSTGRADUATE_PHD, LECTURER, RESEARCHER, OTHER
```

**`gender`** *(optional — user may skip)*
```
MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY
```

**`age_range`** *(optional — user may skip)*
```
UNDER_18, AGE_18_24, AGE_25_34, AGE_35_44, AGE_45_PLUS
```

**`referral_source`**
```
WHATSAPP, SOCIAL_MEDIA, WORD_OF_MOUTH, SEARCH_ENGINE, OTHER
```

### 2.3 Java entity additions

```java
@Entity
@Table(name = "users")
public class User {

    // ... existing fields ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcademicLevel academicLevel;

    @Column
    private Integer yearOfStudy;  // nullable

    @Enumerated(EnumType.STRING)
    @Column
    private Gender gender;        // nullable — user opt-in

    @Enumerated(EnumType.STRING)
    @Column
    private AgeRange ageRange;    // nullable — user opt-in

    @Enumerated(EnumType.STRING)
    @Column
    private ReferralSource referralSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant lastActiveAt;
}
```

### 2.4 Registration request DTO

```java
public class RegisterRequest {
    @NotBlank  private String fullName;
    @Email     private String email;
    @NotBlank  @Size(min = 8) private String password;
    @NotBlank  private String institution;
    @NotBlank  private String fieldOfStudy;
    @NotNull   private AcademicLevel academicLevel;
               private Integer yearOfStudy;       // optional
               private Gender gender;             // optional
               private AgeRange ageRange;         // optional
               private ReferralSource referralSource; // optional
}
```

### 2.5 `last_active_at` update strategy

Update `last_active_at` via a Spring interceptor or filter on every authenticated request — not on every DB write. Keep it lightweight:

```java
@Component
public class ActivityTrackingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // Update asynchronously to avoid adding latency to every request
        userService.updateLastActiveAsync(currentUserId);
        filterChain.doFilter(request, response);
    }
}
```

Use `@Async` on `updateLastActive` and only write if `last_active_at` is older than 5 minutes — avoids a DB write on every single request.

---

## 3. Survey Creation

### 3.1 Schema — `surveys` table

| Field                    | Type                     | Tag           | Notes                                              |
|--------------------------|--------------------------|---------------|----------------------------------------------------|
| `id`                     | `BIGINT` PK              | `[MVP]`       | Auto-generated                                     |
| `owner_id`               | `BIGINT` FK → users      | `[MVP]`       | Survey creator                                     |
| `title`                  | `VARCHAR(255)`           | `[MVP]`       |                                                    |
| `description`            | `TEXT`                   | `[MVP]`       |                                                    |
| `category`               | `VARCHAR(100)`           | `[MVP]`       | e.g. "Health", "Education"                         |
| `estimated_time_minutes` | `SMALLINT`               | `[MVP]`       | Creator's estimate                                 |
| `target_response_count`  | `INT`                    | `[MVP]`       | Creator's goal                                     |
| `actual_response_count`  | `INT`                    | `[MVP]`       | Default 0; incremented atomically on response      |
| `status`                 | `ENUM`                   | `[MVP]`       | See values below                                   |
| `boost_expires_at`       | `TIMESTAMPTZ` NULL       | `[MVP]`       | Null = not boosted or expired                      |
| `target_academic_levels` | `VARCHAR[]` / join table | `[FUTURE]`    | Respondent targeting                               |
| `target_fields_of_study` | `TEXT` NULL              | `[FUTURE]`    | Comma-separated or join table                      |
| `target_institutions`    | `TEXT` NULL              | `[FUTURE]`    | Respondent targeting                               |
| `created_at`             | `TIMESTAMPTZ`            | `[MVP]`       | Set on insert                                      |
| `published_at`           | `TIMESTAMPTZ` NULL       | `[ANALYTICS]` | Set when status → ACTIVE; separate from created_at |
| `closed_at`              | `TIMESTAMPTZ` NULL       | `[ANALYTICS]` | Set when status → CLOSED                           |

### 3.2 Enum values

**`status`**
```
DRAFT, ACTIVE, CLOSED
```

- `DRAFT` — created but not yet published to the feed
- `ACTIVE` — visible in the feed and accepting responses
- `CLOSED` — target reached or manually closed by owner; removed from feed

### 3.3 Java entity additions

```java
@Entity
@Table(name = "surveys")
public class Survey {

    // ... existing fields ...

    @Column(nullable = false)
    private Integer targetResponseCount;

    @Column(nullable = false)
    private Integer actualResponseCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SurveyStatus status = SurveyStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant publishedAt;   // set when status → ACTIVE

    @Column
    private Instant closedAt;      // set when status → CLOSED
}
```

### 3.4 Status transition rules

| From     | To       | Trigger                                                           | Side effect                                    |
|----------|----------|-------------------------------------------------------------------|------------------------------------------------|
| `DRAFT`  | `ACTIVE` | Owner publishes                                                   | Set `published_at = NOW()`                     |
| `ACTIVE` | `CLOSED` | Owner closes, or `actual_response_count >= target_response_count` | Set `closed_at = NOW()`                        |
| `CLOSED` | `ACTIVE` | Owner re-opens                                                    | Clear `closed_at`; do not reset `published_at` |

Auto-close when target is reached: check `actualResponseCount >= targetResponseCount` inside the response submission transaction and transition status atomically.

---

## 4. Survey Responses

### 4.1 Schema — `responses` table

| Field                       | Type                  | Tag           | Notes                                          |
|-----------------------------|-----------------------|---------------|------------------------------------------------|
| `id`                        | `BIGINT` PK           | `[MVP]`       | Auto-generated                                 |
| `survey_id`                 | `BIGINT` FK → surveys | `[MVP]`       |                                                |
| `respondent_id`             | `BIGINT` FK → users   | `[MVP]`       |                                                |
| `responded_at`              | `TIMESTAMPTZ`         | `[MVP]`       | When submission was received                   |
| `time_to_complete_seconds`  | `INT` NULL            | `[ANALYTICS]` | Client-measured; see below                     |
| `respondent_academic_level` | `ENUM`                | `[ANALYTICS]` | Snapshot from user profile at time of response |
| `respondent_field_of_study` | `VARCHAR(255)`        | `[ANALYTICS]` | Snapshot from user profile                     |
| `respondent_institution`    | `VARCHAR(255)`        | `[ANALYTICS]` | Snapshot from user profile                     |

**Unique constraint (MVP — enforced at DB level):**
```sql
UNIQUE (survey_id, respondent_id)
```

### 4.2 Schema — `response_answers` table

Individual answers stored per question, not as a JSON blob. This enables per-question analytics.

| Field          | Type                    | Tag           | Notes                                                      |
|----------------|-------------------------|---------------|------------------------------------------------------------|
| `id`           | `BIGINT` PK             | `[MVP]`       |                                                            |
| `response_id`  | `BIGINT` FK → responses | `[MVP]`       |                                                            |
| `question_id`  | `BIGINT` FK → questions | `[MVP]`       |                                                            |
| `answer_value` | `TEXT`                  | `[MVP]`       | String representation of any answer type                   |
| `answered_at`  | `TIMESTAMPTZ`           | `[ANALYTICS]` | Per-question timestamp; enables time-per-question analysis |

### 4.3 Respondent snapshot — why it matters

User profiles can change (a student graduates, changes institution). Copying `academic_level`, `field_of_study`, and `institution` into the response record at submission time ensures historical accuracy. Survey creators see who *actually* responded, not who those people are *today*.

```java
// Inside response submission service
Response response = Response.builder()
    .survey(survey)
    .respondent(respondent)
    .respondedAt(Instant.now())
    .timeToCompleteSeconds(request.getTimeToCompleteSeconds())
    .respondentAcademicLevel(respondent.getAcademicLevel())   // snapshot
    .respondentFieldOfStudy(respondent.getFieldOfStudy())     // snapshot
    .respondentInstitution(respondent.getInstitution())       // snapshot
    .build();
```

### 4.4 `time_to_complete_seconds` — collection strategy

This field is **client-measured** and sent with the submission payload. The React frontend records a start timestamp when the survey first renders and computes elapsed time on submit:

```js
// Start timing when survey loads
const startTime = useRef(Date.now());

// On submit
const timeToCompleteSeconds = Math.floor((Date.now() - startTime.current) / 1000);

await api.post(`/surveys/${surveyId}/responses`, {
    answers,
    timeToCompleteSeconds,
});
```

**Validation on the backend:** Accept the value but treat it as advisory — cap it at a reasonable maximum (e.g. 7200 seconds / 2 hours) and floor it at 0. Flag responses where `time_to_complete_seconds < (survey.estimated_time_minutes * 60 * 0.2)` as potentially low quality (completed in under 20% of estimated time). Do not reject them at MVP — just store the flag for future fraud detection.

```java
// In response submission service
if (request.getTimeToCompleteSeconds() != null) {
    int capped = Math.min(request.getTimeToCompleteSeconds(), 7200);
    response.setTimeToCompleteSeconds(Math.max(capped, 0));
}
```

---

## 5. Credit Logs

### 5.1 Schema — `credit_logs` table (additions only)

The existing table handles all credit events. Verify these fields are present:

| Field        | Type                       | Tag     | Notes                                      |
|--------------|----------------------------|---------|--------------------------------------------|
| `id`         | `BIGINT` PK                | `[MVP]` |                                            |
| `user_id`    | `BIGINT` FK → users        | `[MVP]` |                                            |
| `amount`     | `INT`                      | `[MVP]` | Positive = earn, negative = spend          |
| `reason`     | `ENUM`                     | `[MVP]` | `SIGNUP_BONUS`, `SURVEY_RESPONSE`, `BOOST` |
| `survey_id`  | `BIGINT` FK NULL → surveys | `[MVP]` | Which survey triggered this event          |
| `created_at` | `TIMESTAMPTZ`              | `[MVP]` | Append-only — no updates or deletes        |

No new fields needed for analytics — this table already produces credit circulation metrics via aggregation queries.

---

## 6. Analytics Queries

The fields above enable the following without any additional schema changes.

### 6.1 For survey creators (per-survey dashboard)

```sql
-- Response rate over time (responses per day)
SELECT DATE(responded_at) AS day, COUNT(*) AS responses
FROM responses
WHERE survey_id = :surveyId
GROUP BY day ORDER BY day;

-- Respondent breakdown by academic level
SELECT respondent_academic_level, COUNT(*) AS count
FROM responses
WHERE survey_id = :surveyId
GROUP BY respondent_academic_level;

-- Average and median completion time
SELECT
    AVG(time_to_complete_seconds) AS avg_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY time_to_complete_seconds) AS median_seconds
FROM responses
WHERE survey_id = :surveyId AND time_to_complete_seconds IS NOT NULL;

-- Time to reach target response count
SELECT
    EXTRACT(EPOCH FROM (closed_at - published_at)) / 3600 AS hours_to_target
FROM surveys
WHERE id = :surveyId AND status = 'CLOSED';
```

### 6.2 Platform-level analytics

```sql
-- Daily active users
SELECT DATE(last_active_at) AS day, COUNT(DISTINCT id) AS dau
FROM users
GROUP BY day ORDER BY day;

-- Credit circulation rate (earned vs spent per day)
SELECT
    DATE(created_at) AS day,
    SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) AS credits_earned,
    SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) AS credits_spent
FROM credit_logs
GROUP BY day ORDER BY day;

-- Boost effectiveness (boosted vs non-boosted time to target)
SELECT
    CASE WHEN boost_expires_at IS NOT NULL THEN 'boosted' ELSE 'not_boosted' END AS boost_type,
    AVG(EXTRACT(EPOCH FROM (closed_at - published_at)) / 3600) AS avg_hours_to_target
FROM surveys
WHERE status = 'CLOSED'
GROUP BY boost_type;

-- Potential fraud signals (suspiciously fast completions)
SELECT r.*, s.estimated_time_minutes
FROM responses r
JOIN surveys s ON r.survey_id = s.id
WHERE r.time_to_complete_seconds < (s.estimated_time_minutes * 60 * 0.2);
```

---

## 7. What Is Explicitly Deferred

The following are **not** collected at MVP. Do not add them now.

| Field                                                       | Reason deferred                                                                    |
|-------------------------------------------------------------|------------------------------------------------------------------------------------|
| Geolocation (beyond institution)                            | No use case until cross-region expansion                                           |
| Device / browser / OS                                       | No mobile-specific features at MVP                                                 |
| In-survey behaviour (mouse movement, focus/blur events)     | Significant frontend complexity; fraud detection can start with `time_to_complete` |
| IP address logging                                          | Privacy implications; needs policy before collection                               |
| `target_academic_levels` / `target_institutions` on surveys | Schema column can be added now (nullable), but matching logic is a future feature  |

---

## 8. Migration Summary

Changes required relative to the current scaffold:

### New columns — `users`
```sql
ALTER TABLE users ADD COLUMN academic_level VARCHAR(50) NOT NULL;
ALTER TABLE users ADD COLUMN year_of_study SMALLINT NULL;
ALTER TABLE users ADD COLUMN gender VARCHAR(30) NULL;
ALTER TABLE users ADD COLUMN age_range VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN referral_source VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN last_active_at TIMESTAMPTZ NULL;
```

### New columns — `surveys`
```sql
ALTER TABLE surveys ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE surveys ADD COLUMN target_response_count INT NULL;
ALTER TABLE surveys ADD COLUMN actual_response_count INT NOT NULL DEFAULT 0;
ALTER TABLE surveys ADD COLUMN published_at TIMESTAMPTZ NULL;
ALTER TABLE surveys ADD COLUMN closed_at TIMESTAMPTZ NULL;
```

### New columns — `responses`
```sql
ALTER TABLE responses ADD COLUMN time_to_complete_seconds INT NULL;
ALTER TABLE responses ADD COLUMN respondent_academic_level VARCHAR(50) NULL;
ALTER TABLE responses ADD COLUMN respondent_field_of_study VARCHAR(255) NULL;
ALTER TABLE responses ADD COLUMN respondent_institution VARCHAR(255) NULL;
```

### New table — `response_answers`
```sql
CREATE TABLE response_answers (
    id BIGSERIAL PRIMARY KEY,
    response_id BIGINT NOT NULL REFERENCES responses(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    answer_value TEXT NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

> **Note:** Per the maintainability NFR, schema is managed by JPA/Hibernate for the MVP. These SQL statements are for reference — the actual changes should be expressed as JPA entity modifications and allowed to auto-generate via `spring.jpa.hibernate.ddl-auto=update` (dev) or a Liquibase/Flyway migration (production).

---

*Spec authored for SurveyExchange MVP — internal reference document.*