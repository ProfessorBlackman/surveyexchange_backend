# API Gaps — Fields & Endpoints Required by Frontend

These are the additions the backend needs to make before the frontend features built against `frontend-data-collection-spec.md` will work end-to-end.

---

## Missing Endpoints

### `PATCH /api/surveys/:id`

**Purpose:** Status transitions — publishing a draft, closing an active survey, and re-opening a closed one.

The frontend sends:
```json
{ "status": "ACTIVE" }   // publish / re-open
{ "status": "CLOSED" }   // close
```

Expected response: the updated `SurveySummaryResponse` (or at minimum HTTP 200).

Only the survey owner should be permitted to call this. Suggested error codes: `403` if the caller is not the owner, `404` if the survey does not exist, `409` if the transition is illegal (e.g. closing an already-closed survey).

---

## Missing Response Body

### `POST /api/surveys/:id/respond` — 201 body

**Purpose:** The frontend updates the user's credit balance immediately from the response payload, avoiding a second round-trip to `GET /credits/balance`.

Currently the 201 response has no documented schema. The backend should return:

```json
{
  "newBalance": 12
}
```

The frontend falls back to `GET /users/me` when `newBalance` is absent, so this is a performance improvement rather than a hard blocker.

---

## Missing Fields on Existing Schemas

### `SurveySummaryResponse`

Returned by `GET /api/surveys/mine` and `GET /api/surveys/feed`.

| Field                 | Type                              | Purpose                                                                                          |
|-----------------------|-----------------------------------|--------------------------------------------------------------------------------------------------|
| `status`              | `"DRAFT" \| "ACTIVE" \| "CLOSED"` | Status badge on survey cards; determines which action button to show (Publish / Close / Re-open) |
| `actualResponseCount` | `integer`                         | Response progress display — `actualResponseCount / targetResponses` with a progress bar          |
| `publishedAt`         | `string (date-time)`              | "Published on …" label on survey cards                                                           |
| `boostExpiresAt`      | `string (date-time)`              | Boost expiry countdown on survey cards (already present on `SurveyResponse` but absent here)     |

---

### `SurveyResponse`

Returned by `GET /api/surveys/:id`.

| Field                 | Type                              | Purpose                                                                            |
|-----------------------|-----------------------------------|------------------------------------------------------------------------------------|
| `status`              | `"DRAFT" \| "ACTIVE" \| "CLOSED"` | Guards the respondent view — a `DRAFT` or `CLOSED` survey should not be answerable |
| `actualResponseCount` | `integer`                         | Shown on the survey detail header alongside `targetResponses`                      |

---

### `CreateSurveyRequest`

Body of `POST /api/surveys`.

| Field    | Type                  | Purpose                                                                                                                                                                                                                            |
|----------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `status` | `"DRAFT" \| "ACTIVE"` | Controls whether the survey is saved as a draft or published immediately. The frontend sends one of two values depending on which button the creator clicks ("Save as Draft" vs "Save & Publish"). Defaults to `DRAFT` if omitted. |

---

## Deferred (Future Scope)

### `PATCH /api/users/me`

Not in the current API spec. Required when the profile editing UI is built (spec section 5). The frontend will send a partial user object (institution, fieldOfStudy, academicLevel, yearOfStudy, gender, ageRange) and expects the updated user profile back.
