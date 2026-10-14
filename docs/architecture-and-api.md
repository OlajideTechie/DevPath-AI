# Architecture and API Contracts

## Architecture

```text
React + TypeScript + Vite
          │ REST/JSON
          ▼
Spring Boot modular monolith
          │ Spring Data JPA + Flyway
          ▼
       PostgreSQL
```

The existing `learner` module owns profile APIs and persistence. The
`curriculum` module owns curated lesson definitions and deterministic
recommendations. The `feedback` module owns code attempts and advisory
feedback. The `recall` module owns persisted competency gaps and the transfer
exercise lifecycle. The `progress` module reports persisted learning activity
and deterministic next steps. Curated lesson content is separate from
application logic. The frontend never calls an LLM provider directly.

## Learner profile schema

Flyway V1 creates `learners`; V2 adds `experience_band` and JSONB
`specializations`. V2 backfills these from the existing numeric experience and
single specialization columns and retains the original columns so existing
profile data is not discarded.

New profiles use these experience bands:

| API value | Display label | Legacy numeric compatibility value |
| --- | --- | ---: |
| `ZERO_TO_ONE` | 0–1 years | 0 |
| `TWO_TO_THREE` | 2–3 years | 2 |
| `FOUR_TO_FIVE` | 4–5 years | 4 |
| `SIX_TO_EIGHT` | 6–8 years | 6 |
| `NINE_PLUS` | 9+ years | 9 |

The numeric compatibility column stores the range's lower bound for new
profiles. It must not be presented as the learner's exact experience. Existing
numeric values remain intact; V2 derives their corresponding band.

`specializations` is a non-empty JSONB array. The legacy scalar
`specialization` column retains its original value for existing profiles and
stores the first selected specialization for new profiles. Technical skills
and study days remain JSONB arrays. Timestamps are stored as `TIMESTAMPTZ`.

Other learner fields remain: UUID id, fixed current role, target role,
preferred language, programming level, study days, daily minutes, learning
style, status, and creation/update timestamps.

## Current learner API

### `POST /api/v1/learners`

The role is server-controlled (`QA Engineer`); target role, language, and
profile status also remain server-controlled.

Request:

```json
{
  "experienceBand": "FOUR_TO_FIVE",
  "specializations": ["API Testing", "Database Testing"],
  "existingSkills": ["Postman", "SQL"],
  "programmingLevel": "BEGINNER",
  "studyDays": ["MONDAY", "WEDNESDAY", "SATURDAY"],
  "dailyMinutes": 45,
  "learningStyle": "PRACTICAL"
}
```

Constraints:

- Experience band is required and must be one of the five values above.
- One to nine unique, nonblank specializations; each is at most 100 characters.
- Up to 20 technical skills, each at most 100 characters; an empty list is
  allowed. Skills are trimmed and deduplicated case-insensitively.
- Programming level: `BEGINNER`, `INTERMEDIATE`, or `EXPERIENCED`.
- Study days: one or more unique uppercase weekdays.
- Daily minutes: 10–240.
- Learning style: `PRACTICAL`, `READING`, or `MIXED`.

Success: `201 Created` and
`Location: /api/v1/learners/{id}`. The creation DTO returns the persisted id,
fixed role/target/language/status, programming level, and timestamp.

Invalid input returns HTTP 400 in the shared error envelope:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid learner profile",
  "fieldErrors": {
    "specializations": "must not be empty"
  }
}
```

### `GET /api/v1/learners/{learnerId}`

Returns the persisted profile, including `experienceBand` and the array
`specializations`. An unknown ID returns HTTP 404
`{"code":"LEARNER_NOT_FOUND","message":"Learner not found"}`. A malformed UUID
returns HTTP 400.

## Health and API docs

- `GET /actuator/health`: minimal public health status.
- `/swagger-ui/index.html`: interactive OpenAPI documentation.
- `/v3/api-docs`: OpenAPI JSON.

## Recommended lesson

`GET /api/v1/learners/{learnerId}/recommended-lesson` returns the current
curated starter, `Java Methods and Input Validation`. Since this is the only
curated lesson in Phase 2, all profiles receive it. The Java focus is selected
from the persisted programming level, and the banking QA scenario is selected
from both the learner's persisted experience band and specializations. Lower
experience bands receive concrete, straightforward cases; higher bands
receive progressively more layered regression, boundary, and contract
coverage. QA tenure is not used as a proxy for Java proficiency. The dashboard
and lesson workspace show these personalized details. The response also
provides a short summary, learning objective, and estimated reading/practice
time. Unknown learners and malformed IDs use the existing shared 404 and 400
error conventions.

## Curated lesson content

`GET /api/v1/lessons/{lessonId}` returns the available curated lesson
explanation and practice exercise. The current lesson is
`java-methods-input-validation` (**Java Methods and Input Validation**). Its
response contains the banking API QA context, three lesson sections, objective,
exercise requirements, cases to consider, and a Java starter template. Unknown
IDs return HTTP 404 with `LESSON_NOT_FOUND`.

The browser holds a draft only while the lesson page remains mounted. Phase 4
adds a submission API and persists the code and advisory feedback; code is
never compiled or executed.

## Code submission and advisory feedback

`POST /api/v1/learners/{learnerId}/lessons/{lessonId}/submissions` accepts
nonblank Java source up to 20,000 characters. The backend validates the learner
and curated lesson, stores a `learning_attempts` row, calls an
OpenAI-compatible chat completions endpoint using backend-only configuration,
validates the structured response and stable competency tags, persists it as
JSONB, and returns the attempt ID and feedback. Provider failure returns HTTP
503 with `AI_FEEDBACK_UNAVAILABLE`; the attempt remains recorded with
`FEEDBACK_FAILED`.

The evaluation prompt includes the exercise requirements and acceptance cases.
Structured findings have categories `FUNCTIONAL_DEFECT`,
`OPTIONAL_IMPROVEMENT`, `ADDITIONAL_DEFENSIVE_CHECK`, or
`PLANNED_REINFORCEMENT`, and each finding names a stable competency identifier.
The assessment also returns confidence and a typed next-action category
(`TARGETED_CORRECTION`, `REINFORCEMENT`, `NEXT_LESSON`, or `CLARIFICATION`).
The backend rejects inconsistent responses: functional defects require
`NEEDS_REVIEW` and `TARGETED_CORRECTION`, while low confidence requires
`UNCERTAIN` and `CLARIFICATION` and cannot create competency gaps. Missing
requirements must produce a targeted correction; satisfied requirements lead
to reinforcement or the next lesson. Additional harmless checks are not
defects.
For example, rejecting a negative available balance is optional defensive
validation in the withdrawal exercise and must not lower the recommendation.
This remains advisory only and cannot establish mastery. The response
explicitly sets `codeExecutionPerformed: false`.

Set `AI_PROVIDER_API_KEY` only in the backend environment. The provider base
URL, model, connection timeout, and request timeout are configurable with
`AI_PROVIDER_BASE_URL`, `AI_PROVIDER_MODEL`,
`AI_PROVIDER_CONNECT_TIMEOUT`, and `AI_PROVIDER_REQUEST_TIMEOUT`. No secret is
exposed to frontend configuration.

For local demonstrations during provider downtime, `AI_DEMO_MODE=true` selects
a separate deterministic sample provider. Its feedback carries
`feedbackSource: "DEMO_SAMPLE"`, is explicitly labeled in the UI, and is always
`UNCERTAIN` with low confidence and no findings. It is not based on or an
assessment of submitted code and creates no Recall evidence. The default is
`false`; provider errors continue to return HTTP 503 unless demo mode is
explicitly enabled. Switch it off to restore live provider requests.

## Learning Recall

Flyway V4 creates `learner_competency_recall`, keyed by learner and stable
competency tag; V5 adds a reason (`COMPETENCY_GAP` or
`PLANNED_REINFORCEMENT`). A starter-exercise finding categorized as
`FUNCTIONAL_DEFECT` creates or refreshes a `NEEDS_REVIEW` gap. A finding
categorized as `PLANNED_REINFORCEMENT` may create `PRACTICING` recall work.
Broad competency tags, optional improvements, additional defensive checks, and
uncertain assessments do not create competency gaps.
The curated follow-up is a bank-transfer validation task, a different context
from the original withdrawal exercise. Recall submissions are persisted in
`learning_attempts` with lesson ID `bank-transfer-validation-recall`.

### `GET /api/v1/learners/{learnerId}/recall`

Returns all supported competency tags and their persisted status
(`NOT_STARTED`, `NEEDS_REVIEW`, `PRACTICING`, or `DEMONSTRATED`), related
attempt IDs when available, the curated transfer exercise, and the assessment
policy. Each persisted competency includes its original gap-causing
`sourceAttempt`: attempt and lesson IDs, submitted code, persisted advisory
feedback, and submission time. This lets the learner review the exact work and
feedback that led to the recall exercise.

### `POST /api/v1/learners/{learnerId}/recall/submissions`

Request:

```json
{
  "competencyTag": "INPUT_VALIDATION",
  "code": "public class TransferValidator { ... }"
}
```

The tag must have an active persisted gap. The code is saved and sent to the
configured feedback provider. A positive transfer review requires both a
`READY_FOR_REVIEW` recommendation and the selected competency tag; it moves
the status to `PRACTICING`, not `DEMONSTRATED`. `READY_FOR_REVIEW` is an
advisory judgment and does not verify the code as correct or executed.

### `POST /api/v1/learners/{learnerId}/recall/{competencyTag}/demonstration`

Request: `{"confirmed": true}`. A competency becomes `DEMONSTRATED` only when
the original and distinct transfer exercises each have persisted positive
advisory recommendations identifying that competency and the learner confirms
they can explain and apply the rule. One AI judgment alone cannot establish
demonstrated status. This is a defined advisory-evidence policy, not proof of
code execution or mastery.

Unsupported tags return HTTP 400, a missing/inactive competency returns 404,
and incomplete evidence returns HTTP 409.

## Learning progress

### `GET /api/v1/learners/{learnerId}/progress`

Returns persisted progress only: the number of distinct lesson IDs with
attempts, total saved exercise submissions, persisted competency records
grouped into `competenciesNeedingReview`, `competenciesInProgress`, and
`competenciesDemonstrated`, the most recent attempt with ready feedback, and a
deterministic next activity. Failed-feedback attempts count as submitted
exercises and attempted lessons but are not returned as feedback. Competencies
without a persisted recall record are omitted rather than treated as completed.

`latestFeedback` is `null` when no persisted attempt has ready feedback. It
contains the attempt and lesson IDs, submission time, feedback summary, next
action, competency tags, and review recommendation; submitted code is never
included.

Next activity selection uses persisted competency evidence in this order:
awaiting learner confirmation for a positive two-exercise evidence pair, a
competency needing review, unfinished recall practice, then the curated
starter lesson. If recall evidence is incomplete because the original
exercise still needs a positive review, the starter lesson is recommended to
complete that evidence pair. With no submissions, the starter lesson is
recommended as the starting activity. This rule does not infer scores,
streaks, code execution, or mastery.

The endpoint only reads persisted attempts and recall state; it does not call
the feedback provider or create progress records. Unknown learners and
malformed IDs use the shared 404 and 400 error conventions.

AI assessment remains advisory; actual compilation or test execution must not
be claimed without an execution environment.

## Security boundary

There is no authentication or ownership enforcement. UUIDs are not access
control. Run the capstone locally with synthetic learner data only; do not use
private real-user profiles.
