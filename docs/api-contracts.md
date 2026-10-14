# API Contracts — Capstone

This file records implemented contracts and planned additions before those
additions are implemented. The current learner API uses `/api/v1`.

## Implemented: create learner

`POST /api/v1/learners`

### Request

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

`experienceBand` is one of `ZERO_TO_ONE`, `TWO_TO_THREE`, `FOUR_TO_FIVE`,
`SIX_TO_EIGHT`, or `NINE_PLUS`. `specializations` contains one or more unique
nonblank values (maximum nine). `existingSkills` may be an empty array and
contains at most 20 values. Current role (`QA Engineer`), target role, language,
and profile status are set by the server and are not accepted as request
properties.

### Success

- HTTP `201 Created`
- `Location: /api/v1/learners/{id}`
- Body: id, current role, target role, preferred language, programming level,
  profile status, created timestamp.

### Validation error

- HTTP `400 Bad Request`
- `{"code":"VALIDATION_ERROR","message":"Invalid learner profile","fieldErrors":{...}}`

## Implemented: retrieve learner

`GET /api/v1/learners/{learnerId}` returns the persisted profile with
`experienceBand` and `specializations` array. Unknown UUID: HTTP 404 with
`LEARNER_NOT_FOUND`. Malformed UUID: HTTP 400 with `VALIDATION_ERROR`.

## Implemented: recommended starting lesson

`GET /api/v1/learners/{learnerId}/recommended-lesson` returns the curated
starting lesson and deterministic personalization fields:

```json
{
  "lessonId": "java-methods-input-validation",
  "title": "Java Methods and Input Validation",
  "summary": "Learn how Java methods organize business rules and how to validate input before processing a request.",
  "learningObjective": "Write a focused Java method that rejects invalid withdrawal requests before changing an account balance.",
  "estimatedMinutes": 20,
  "recommendationReason": "Start with one focused Java method: identify its inputs, check for missing or non-positive values, then compare the amount.",
  "qaScenario": "Build a focused regression set for null, non-positive, exact-balance, and over-balance requests. Apply your API testing experience to check the withdrawal endpoint's accepted and rejected responses."
}
```

The recommendation selects the first and currently only curated lesson for all
profiles. Its Java focus is selected by programming level. The QA scenario is
selected deterministically from the learner's experience band and QA
specializations: the lowest bands receive clear, concrete test cases, and
higher bands receive progressively more layered regression and contract
coverage. QA tenure does not determine Java proficiency. Unknown learners
return the shared `LEARNER_NOT_FOUND` 404 response; malformed UUIDs return the
shared `VALIDATION_ERROR` 400 response.

## Implemented: curated lesson content

`GET /api/v1/lessons/{lessonId}` returns the available curated lesson
explanation and exercise. The current ID is
`java-methods-input-validation`. The response includes a QA banking context,
lesson sections with optional code examples, learning objective, and practice
exercise containing requirements, suggested cases, and starter Java code.
Unknown lesson IDs return HTTP 404 with
`{"code":"LESSON_NOT_FOUND","message":"Lesson not found"}`.

The editable frontend draft is held in page memory until submitted. It is not
saved locally or compiled/executed.

## Implemented: code submission and advisory feedback

`POST /api/v1/learners/{learnerId}/lessons/{lessonId}/submissions`

Request:

```json
{
  "code": "public class WithdrawalValidator { ... }"
}
```

The code is required, nonblank, and limited to 20,000 characters. The learner
must exist and the lesson ID must identify a curated lesson. A valid submission
is recorded with a persisted advisory feedback response before success is
returned.

Success: HTTP `201 Created`

```json
{
  "attemptId": "uuid",
  "learnerId": "uuid",
  "lessonId": "java-methods-input-validation",
  "feedback": {
    "summary": "Review summary",
    "strengths": ["A specific strength"],
    "issues": ["A missing case"],
    "improvements": ["A suggested improvement"],
    "findings": [{
      "category": "FUNCTIONAL_DEFECT",
      "competency": "INPUT_VALIDATION",
      "description": "The requested amount can be negative."
    }],
    "assessmentConfidence": "HIGH",
    "competencyTags": ["INPUT_VALIDATION"],
    "nextActionType": "TARGETED_CORRECTION",
    "nextAction": "Add the missing input guard",
    "reviewRecommendation": "NEEDS_REVIEW",
    "feedbackSource": "LIVE_PROVIDER"
  },
  "advisoryOnly": true,
  "codeExecutionPerformed": false,
  "submittedAt": "2026-10-12T15:00:00Z"
}
```

Supported finding categories are `FUNCTIONAL_DEFECT`, `OPTIONAL_IMPROVEMENT`,
`ADDITIONAL_DEFENSIVE_CHECK`, and `PLANNED_REINFORCEMENT`. Each finding uses a
competency identifier from `JAVA_METHODS`, `INPUT_VALIDATION`,
`BOUNDARY_CONDITIONS`, `DECIMAL_MONEY`, or `NULL_HANDLING`. Assessment
confidence is `HIGH`, `MEDIUM`, or `LOW`; review recommendations are
`NEEDS_REVIEW`, `CONTINUE_PRACTICE`, `READY_FOR_REVIEW`, or `UNCERTAIN`.
Next-action types are `TARGETED_CORRECTION`, `REINFORCEMENT`, `NEXT_LESSON`,
and `CLARIFICATION`. Functional defects must align with `NEEDS_REVIEW` and a
targeted correction. Low-confidence output must be `UNCERTAIN` with a
clarification action and cannot contain findings or create competency gaps.
Without defects, the action must be reinforcement or a next lesson. `issues`
and `improvements` may be empty when requirements are met. Unsupported legacy
competency tags from the provider are discarded, but invalid categories,
inconsistent competency references, and contradictory assessment states are
rejected.

Evaluation is grounded in the exercise requirements and acceptance cases.
Harmless additional validation is not a defect; the withdrawal exercise does
not require rejecting a negative available balance. Missing requirements
produce a specific correction, satisfied requirements lead to reinforcement
or the next lesson, and uncertain assessments explicitly ask for clarification
or a complete submission. These recommendations are advisory: the code is not
compiled or executed.

Invalid input returns HTTP 400 using `VALIDATION_ERROR`. Unknown learner or
lesson returns HTTP 404 using `LEARNER_NOT_FOUND` or `LESSON_NOT_FOUND`.
Unconfigured, failed, timed-out, or invalid provider responses return HTTP 503
with `AI_FEEDBACK_UNAVAILABLE`. The attempt is retained with status
`FEEDBACK_FAILED` when feedback cannot be obtained.

The API key and model configuration are backend-only environment variables:
`AI_PROVIDER_API_KEY`, `AI_PROVIDER_BASE_URL`, `AI_PROVIDER_MODEL`,
`AI_PROVIDER_CONNECT_TIMEOUT`, and `AI_PROVIDER_REQUEST_TIMEOUT`. The default
client speaks the OpenAI-compatible chat completions JSON format. Provider
requests are made only by the backend and responses are schema/size validated.

## Implemented: Learning Recall

Flyway V4 persists one recall record per learner and competency. Flyway V5
adds a reason: `COMPETENCY_GAP` or `PLANNED_REINFORCEMENT`. Only explicitly
categorized functional defects create `NEEDS_REVIEW` gaps; deliberate planned
reinforcement findings may create `PRACTICING` work. Broad competency tags,
optional improvements, additional defensive checks, and uncertain feedback
alone do not generate Recall entries. Older persisted feedback is returned
with empty findings, `MEDIUM` confidence, and a next-action type inferred from
its existing review recommendation; this compatibility mapping does not
retroactively create or remove Recall records. Missing records are returned as
`NOT_STARTED`.

`GET /api/v1/learners/{learnerId}/recall` returns the learner's five supported
competency statuses, related attempt IDs, an advisory assessment policy, and
the curated bank-transfer exercise. Persisted competency entries also include
`sourceAttempt`, with the exact original submitted code and its saved feedback:

```json
{
  "competencyTag": "INPUT_VALIDATION",
  "status": "NEEDS_REVIEW",
  "gapAttemptId": "uuid",
  "reason": "COMPETENCY_GAP",
  "sourceAttempt": {
    "attemptId": "uuid",
    "lessonId": "java-methods-input-validation",
    "submittedCode": "public class WithdrawalValidator { ... }",
    "feedback": {
      "summary": "Review summary",
      "strengths": ["A specific strength"],
      "issues": ["A missing case"],
      "improvements": ["A suggested improvement"],
      "findings": [{
        "category": "FUNCTIONAL_DEFECT",
        "competency": "INPUT_VALIDATION",
        "description": "The exercise requires false for null arguments."
      }],
      "assessmentConfidence": "HIGH",
      "competencyTags": ["INPUT_VALIDATION"],
      "nextActionType": "TARGETED_CORRECTION",
      "nextAction": "Add the missing input guard",
      "reviewRecommendation": "NEEDS_REVIEW",
      "feedbackSource": "LIVE_PROVIDER"
    },
    "submittedAt": "2026-10-12T15:00:00Z"
  }
}
```

Each persisted competency includes `reason` (`COMPETENCY_GAP` or
`PLANNED_REINFORCEMENT`). Competencies without a persisted recommendation have
`reason: null` and `sourceAttempt: null`. A
`READY_FOR_REVIEW` recommendation is positive advisory feedback only; it does
not claim compilation, execution, or verified correctness. On Recall,
demonstration still requires positive feedback for the competency in both
exercise contexts and explicit learner confirmation.

`POST /api/v1/learners/{learnerId}/recall/submissions` accepts:

```json
{
  "competencyTag": "INPUT_VALIDATION",
  "code": "public class TransferValidator { ... }"
}
```

The tag must have an active gap. The source is stored as a learning attempt
associated with lesson ID `bank-transfer-validation-recall`; it is sent to the
same backend-only feedback provider and is never compiled or executed. The
status moves to `PRACTICING` when feedback recommends `READY_FOR_REVIEW` and
includes the selected competency. Otherwise it returns to `NEEDS_REVIEW`.

`POST /api/v1/learners/{learnerId}/recall/{competencyTag}/demonstration`
accepts `{"confirmed":true}`. The explicit policy requires a positive
`READY_FOR_REVIEW` recommendation naming the competency on both the original
withdrawal exercise and the distinct transfer exercise, followed by learner
confirmation. This avoids treating one model judgment as sufficient evidence
for `DEMONSTRATED`; it does not claim code execution or mastery. Incomplete
evidence returns HTTP 409 with `RECALL_EVIDENCE_REQUIRED`.

## Learning progress

`GET /api/v1/learners/{learnerId}/progress` returns persisted learner activity:

```json
{
  "learnerId": "d69d5da5-5652-41aa-b573-cc5b42f5a605",
  "lessonsAttempted": 2,
  "exercisesSubmitted": 3,
  "competenciesNeedingReview": [
    {
      "competencyTag": "NULL_HANDLING",
      "status": "NEEDS_REVIEW",
      "updatedAt": "2026-10-12T15:00:00Z"
    }
  ],
  "competenciesInProgress": [],
  "competenciesDemonstrated": [],
  "latestFeedback": {
    "attemptId": "e9db49a4-6e9b-49ea-893d-256172b090cc",
    "lessonId": "java-methods-input-validation",
    "submittedAt": "2026-10-12T15:00:00Z",
    "summary": "The validation behavior was reviewed.",
    "nextAction": "Review the boundary cases.",
    "competencyTags": ["NULL_HANDLING"],
    "reviewRecommendation": "NEEDS_REVIEW",
    "feedbackSource": "LIVE_PROVIDER"
  },
  "nextActivity": {
    "type": "RECALL",
    "lessonId": "bank-transfer-validation-recall",
    "title": "Recall: Validate a Bank Transfer",
    "estimatedMinutes": 15,
    "reason": "Practise the transfer exercise for NULL_HANDLING, which is currently marked NEEDS_REVIEW.",
    "competencyTag": "NULL_HANDLING"
  }
}
```

`lessonsAttempted` counts distinct lesson IDs across saved attempts;
`exercisesSubmitted` counts all persisted submissions, including attempts
whose feedback request failed. `latestFeedback` is the latest persisted
`FEEDBACK_READY` result or `null`. Competency arrays contain only persisted
recall records. `nextActivity` follows the deterministic evidence-priority
rules in the architecture contract and never implies code execution or
mastery.
