## 1. Product Overview

The backend powers:

- Authentication (JWT)
- Survey lifecycle
- Credit economy
- Feed ranking logic

Built with:

- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis (caching)

---

## 2. Architecture

### Layered Architecture (MANDATORY)

```
Controller → Service → Repository
```

No business logic in controllers

---

## 3. Core Modules

### 3.1 Authentication Module

#### Features

- Register (with +10 credits)
- Login (JWT, 24h expiry)
- JWT validation filter

#### Security

- BCrypt password hashing
- JWT secret ≥ 256 bits

---

### 3.2 User Module

#### Fields

- id
- email
- password
- name
- institution
- fieldOfStudy
- creditBalance

---

### 3.3 Survey Module

#### Entities

- Survey
- Question
- Response
- Answer

#### Features

- Create survey
- Get survey details
- Get user surveys

---

### 3.4 Feed Module

#### Requirements

- Exclude user's own surveys
- Exclude already answered surveys
- Boosted surveys first (DB-level sorting)
- Pagination (10/page)

#### Implementation

JPQL:

```sql
ORDER BY 
  CASE WHEN boostExpiry > now() THEN 1 ELSE 0 END DESC,
  createdAt DESC
```

---

### 3.5 Response Module

#### Requirements

- One response per user per survey
- Enforced at DB level (unique index)

```sql
UNIQUE(user_id, survey_id)
```

---

### 3.6 Credit System (CRITICAL)

#### Actions

|Action|Credits|
|---|---|
|Signup|+10|
|Response|+1|
|Boost|-5|

#### Requirements

- Immutable transaction log
- Atomic operations (VERY IMPORTANT)

#### Implementation

Use:

```java
@Transactional
```

Atomic flow:

1. Save response
2. Increment credits
3. Insert transaction log

ALL OR NOTHING

---

### 3.7 Boosting Module

#### Requirements

- Deduct 5 credits
- Set boostExpiry = now + 24h
- Reject if insufficient balance

---

### 3.8 Credit Transaction Log

#### Fields

- id
- userId
- amount (+/-)
- type (EARN/SPEND)
- createdAt

#### Rule

- Append-only (never update/delete)

---

## 4. Database Design

### Key Tables

- users
- surveys
- questions
- responses
- answers
- credit_transactions

---

## 5. Redis Usage

### Use Cases

- Feed caching (per page + filter)
- Survey detail caching
- JWT blacklist (optional logout support)

### Strategy

- TTL: 60–120 seconds for feed
- Cache invalidation on:
    - New survey
    - Boost
    - Response submission

---

## 6. API Design

Base:

```
/api
```

Matches existing structure

### Key Endpoints

#### Auth

- POST `/auth/register`
- POST `/auth/login`

#### Surveys

- GET `/surveys/feed`
- GET `/surveys/mine`
- POST `/surveys`
- GET `/surveys/{id}`
- POST `/surveys/{id}/respond`
- POST `/surveys/{id}/boost`

#### Credits

- GET `/surveys/credits/history`

---

## 7. Security

- JWT-based auth
- Role-ready architecture (future RBAC)
- CORS restricted to frontend origin

---

## 8. Performance Requirements

- Feed < 500ms
- DB-level sorting (no in-memory sort)
- Efficient indexes:
    - survey(boostExpiry)
    - response(user_id, survey_id)

---

## 9. Error Handling

Standard responses:

- 400 → validation errors
- 401 → unauthorized
- 403 → forbidden
- 404 → not found

---

## 10. Future Considerations

- RBAC (Admin / Moderator)
- Analytics service
- Notification system
- Fraud detection (credit abuse)

---

## 11. Success Metrics

- API latency
- Error rate
- Credit consistency (no mismatches)
- DB query performance

---

# Final Note (Important)

You’ve already made one very good call in your requirements:  
the **data integrity rules** (atomic credits, unique response constraint, append-only logs) are the backbone of this system.

If those break, your whole economy collapses.

Everything else (UI polish, caching, even performance) is secondary.

---