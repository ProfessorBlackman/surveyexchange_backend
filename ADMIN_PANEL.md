# Admin Control Dashboard PRD — SurveyXchange

## 1. Product Overview

The Admin Control Dashboard is an internal operational interface for managing and monitoring the [SurveyXchange](https://surveys.methuselah.site/?utm_source=chatgpt.com) platform.

The dashboard will be embedded directly inside the existing Spring Boot backend application using server-side rendering with a template engine.

The purpose of the dashboard is to:

* provide operational visibility
* moderate platform activity
* manage users and surveys
* inspect credit transactions
* monitor system health
* support debugging and maintenance

This is an internal tool intended only for administrators.

The dashboard complements the existing layered backend architecture already defined for the platform.  

---

# 2. Goals

## Primary Goals

* Centralized operational control
* Real-time visibility into system activity
* Simple deployment (same Spring Boot app)
* Fast internal tooling iteration
* Reduced debugging overhead

## Secondary Goals

* Moderate inappropriate content
* Detect credit abuse or fraud
* Track system performance
* Enable manual recovery operations

---

# 3. Non-Goals (v1)

The first version will NOT include:

* Full RBAC permissions matrix
* Multi-admin collaboration workflows
* Advanced BI analytics
* Audit replay system
* Real-time websocket infrastructure
* Separate frontend application

---

# 4. Tech Stack

| Layer            | Technology      |
| ---------------- | --------------- |
| Backend          | Spring Boot     |
| Rendering        | Thymeleaf       |
| UI Interactivity | HTMX            |
| Styling          | Tabler          |
| Charts           | Chart.js        |
| Security         | Spring Security |
| Database         | PostgreSQL      |

---

# 5. High-Level Architecture

```text id="6hyy1z"
Browser
   ↓
Spring MVC Controller
   ↓
Admin Service Layer
   ↓
Repositories
   ↓
PostgreSQL
```

---

# 6. Module Structure

```text id="ot9v0f"
src/main/java/com/surveyexchange/admin/
├── controller/
├── service/
├── dto/
├── viewmodel/
├── mapper/
└── security/
```

Templates:

```text id="l9v5lj"
src/main/resources/templates/admin/
├── dashboard.html
├── login.html
├── users/
├── surveys/
├── transactions/
├── analytics/
└── system/
```

---

# 7. Authentication & Authorization

## Requirements

* Admin dashboard must be isolated from public user routes
* Only ADMIN users may access dashboard endpoints
* Dashboard routes must require authentication
* Unauthorized access attempts must return HTTP 403

## Roles

```java id="g17kr6"
USER
ADMIN
```

## Protected Route Prefix

```text id="n9b7b0"
/admin/**
```

## Security Requirements

* Passwords remain BCrypt hashed 
* Session timeout configurable
* CSRF protection enabled
* Secure cookies enabled in production

---

# 8. Functional Requirements

# 8.1 Admin Authentication

## Features

* Admin login page
* Logout functionality
* Session persistence
* Unauthorized access redirect

## UI Pages

* `/admin/login`

---

# 8.2 Dashboard Overview

## Purpose

Provide a high-level operational summary of the platform.

## Metrics

* Total users
* Total surveys
* Total responses
* Active boosts
* Credits in circulation
* New users today
* Surveys completed today
* Response rate trends

## Widgets

* Recent activity feed
* Latest surveys
* Recent registrations
* Failed operations
* Quick actions panel

## Charts

* Responses over time
* Survey creation trend
* Credit economy trend

---

# 8.3 User Management

## Features

### User Listing

Admin can:

* view all users
* paginate users
* search users
* filter users

### User Details

Admin can view:

* profile information
* institution
* field of study
* surveys created
* responses submitted
* current credit balance
* boost history
* transaction history

### Administrative Actions

Admin can:

* suspend user
* reactivate user
* manually adjust credits
* view audit history
* impersonate user (optional v1.1)

## Restrictions

* Credit transaction records must remain immutable 
* Adjustments create compensating transactions only

---

# 8.4 Survey Management

## Features

### Survey Listing

Admin can:

* view all surveys
* search surveys
* filter by category
* filter boosted surveys
* sort by activity

### Survey Detail View

Admin can inspect:

* survey metadata
* questions
* responses
* response distribution
* owner information

### Administrative Actions

Admin can:

* delete surveys
* disable surveys
* manually boost surveys
* remove boosts
* feature surveys

---

# 8.5 Credit Transactions

## Purpose

Provide visibility into the platform credit economy.

## Features

### Transaction Listing

Admin can:

* view all transactions
* filter by user
* filter by type
* filter by date range

### Transaction Types

* SIGNUP_BONUS
* SURVEY_RESPONSE_REWARD
* BOOST_PURCHASE
* ADMIN_ADJUSTMENT

## Rules

* Transactions are append-only 
* Transactions cannot be edited
* Transactions cannot be deleted

## Analytics

Dashboard should display:

* credits earned daily
* credits spent daily
* net economy growth
* suspicious activity patterns

---

# 8.6 Analytics Module

## Features

### Platform Analytics

* User growth trend
* Survey creation trend
* Completion rate
* Most active categories
* Most active institutions

### Survey Analytics

* Average completion time
* Dropoff rate
* Boost conversion effectiveness

## Visualization

Use Chart.js for:

* line charts
* pie charts
* bar charts

---

# 8.7 System Monitoring

## Features

### JVM Metrics

* Heap usage
* CPU usage
* Thread count

### Application Metrics

* API response times
* Error rates
* Active sessions

### Database Metrics

* Connection pool usage
* Query latency

### Scheduled Tasks

* boost expiry jobs
* cleanup jobs
* background processing jobs

---

# 8.8 Audit Logging

## Purpose

Track all administrative actions.

## Logged Events

* survey deletion
* user suspension
* credit adjustment
* admin login
* boost modification

## Audit Record Fields

| Field      | Description                 |
| ---------- | --------------------------- |
| adminId    | Acting administrator        |
| action     | Operation performed         |
| targetType | USER / SURVEY / TRANSACTION |
| targetId   | Entity affected             |
| timestamp  | Action time                 |
| metadata   | Additional JSON data        |

---

# 9. UI/UX Requirements

## Layout

Dashboard must include:

* sidebar navigation
* top navigation bar
* responsive tables
* mobile-friendly layout

## Design Principles

* operational clarity over visual flair
* minimal clicks
* high information density
* fast page rendering

## Responsiveness

* desktop-first
* tablet supported
* limited mobile support acceptable for v1

---

# 10. Performance Requirements

## Requirements

* Dashboard pages load within 1 second
* Tables must use server-side pagination
* Charts must lazy load data
* Large datasets must not render fully at once

## Query Requirements

* No in-memory sorting for large datasets
* Database indexes required for:

  * users(email)
  * surveys(category)
  * transactions(created_at)
  * responses(user_id, survey_id)

---

# 11. Security Requirements

## Access Control

* ADMIN role required
* Public users cannot access admin endpoints

## Logging

* All failed admin logins logged
* Suspicious actions logged

## Data Protection

* Sensitive data masked where appropriate
* No plaintext passwords ever exposed 

---

# 12. Error Handling

## Requirements

Dashboard must display:

* user-friendly error pages
* validation messages
* retry options where appropriate

## HTTP Codes

| Code | Meaning          |
| ---- | ---------------- |
| 400  | Validation error |
| 401  | Unauthorized     |
| 403  | Forbidden        |
| 404  | Not found        |
| 500  | Internal error   |

Aligned with existing backend requirements. 

---

# 13. Suggested Database Additions

## admin_audit_logs

| Field       | Type      |
| ----------- | --------- |
| id          | UUID      |
| admin_id    | UUID      |
| action      | VARCHAR   |
| target_type | VARCHAR   |
| target_id   | UUID      |
| metadata    | JSONB     |
| created_at  | TIMESTAMP |

## user_status

Add:

```text id="jqglqj"
ACTIVE
SUSPENDED
```

---

# 14. Suggested Spring Endpoints

## Dashboard

| Method | Endpoint |
| ------ | -------- |
| GET    | `/admin` |

## Users

| Method | Endpoint                    |
| ------ | --------------------------- |
| GET    | `/admin/users`              |
| GET    | `/admin/users/{id}`         |
| POST   | `/admin/users/{id}/suspend` |
| POST   | `/admin/users/{id}/credits` |

## Surveys

| Method | Endpoint                      |
| ------ | ----------------------------- |
| GET    | `/admin/surveys`              |
| GET    | `/admin/surveys/{id}`         |
| POST   | `/admin/surveys/{id}/disable` |
| DELETE | `/admin/surveys/{id}`         |

## Transactions

| Method | Endpoint              |
| ------ | --------------------- |
| GET    | `/admin/transactions` |

## Analytics

| Method | Endpoint           |
| ------ | ------------------ |
| GET    | `/admin/analytics` |

## System

| Method | Endpoint        |
| ------ | --------------- |
| GET    | `/admin/system` |

---

# 15. Future Enhancements

## v1.1

* Admin impersonation
* Real-time notifications
* Moderation queue

## v2

* Full RBAC
* Separate admin frontend
* Multi-admin collaboration
* Fraud detection engine

---

# 16. Success Metrics

## Operational Metrics

* Faster issue resolution time
* Reduced production debugging effort
* Reduced admin workload

## Technical Metrics

* Dashboard response time
* Error rate
* Query performance

## Business Metrics

* Reduced abuse
* Improved moderation
* Better survey quality

---

# 17. Final Notes

The admin dashboard is intended to become the operational backbone of the platform.

The initial implementation should prioritize:

1. visibility
2. operational control
3. reliability
4. simplicity

over visual polish or frontend complexity.

Embedding the dashboard directly into the Spring Boot application is intentionally aligned with the platform’s current architecture and MVP stage. 
