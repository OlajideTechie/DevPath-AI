# Codex Worklog

## Phase 1 — QA-focused onboarding

- **Objective:** Extend the existing onboarding flow for QA learners without
  replacing the landing page or creating a parallel app.
- **Implementation summary:** Fixed QA role, categorical experience bands,
  searchable multi-select QA specializations, optional technical skills,
  updated learner API/model, and additive Flyway migration are implemented.
- **Key engineering decisions:** New profile requests use `experienceBand`.
  The legacy numeric years column is retained; new rows store the selected
  band's lower bound there, while existing exact values are preserved. V2 maps
  existing numeric values into bands and wraps old specializations into JSONB.
- **Tests executed:** Frontend build and lint passed. Backend package build
  passed with `-DskipTests` (main and test sources compiled; tests not run).
  `git diff --check` passed. The user asked not to run tests, so neither
  frontend nor backend test suites were executed.
- **Issues encountered:** Older product/API docs described the earlier M1-only
  deployment scope and numeric single-specialization contract.
- **Fixes applied:** Updated product, UI-flow, architecture, and API-contract
  docs to reflect the local capstone direction and categorical multi-select
  onboarding. Updated README and progress/worklog to record the new local
  scope and actual verification.
- **Remaining risks:** The API contract changes the shape of learner creation
  and retrieval within `/api/v1`; this is acceptable for the local capstone but
  would require versioning or compatibility handling for external clients.
  Later learning, AI, recall, and progress phases remain unimplemented.

## Phase 2 — Personalized learning entry

- **Objective:** Give a newly onboarded learner an honest, QA-contextualized
  next step without inventing learning activity or mastery.
- **Implementation summary:** Added a dashboard route, saved profile context,
  a curated starter-lesson recommendation endpoint, and an onboarding success
  link to the dashboard. The lesson workspace, activity tracking, and Recall
  remain clearly marked as upcoming/unavailable.
- **Recommendation rules:** The current catalog contains one starter lesson,
  so it is selected for all profiles. Programming level determines the
  recommendation explanation. A deterministic specialization match selects
  the banking QA scenario.
- **Tests executed:** All 14 frontend tests passed. The two
  `LessonRecommendationControllerTest` cases passed, followed by the full
  17-test backend suite against local PostgreSQL. Frontend build/lint and
  backend package compilation passed; `git diff --check` passed.
- **Remaining risks:** A separate V2 legacy-row backfill test against a
  pre-migration schema has not been run. Lesson content is only a
  recommendation preview; Phase 3 must build the actual lesson/practice
  workspace.

## Phase 4 — AI-assisted feedback

- **Objective:** Accept bounded exercise submissions, persist attempts and
  structured feedback, and return controlled retryable errors without ever
  claiming code execution.
- **Implementation summary:** Added the learner-scoped submission endpoint,
  Flyway V3 `learning_attempts` table, backend-only OpenAI-compatible client,
  structured response validation, retryable lesson UI, and persisted feedback
  display. Provider failures retain the attempt with `FEEDBACK_FAILED`.
- **Security and trust boundary:** The browser sends source only to the
  application backend; provider keys are environment-only on the backend.
  Submissions are limited to 20,000 characters, provider calls have configured
  timeouts and response-size bounds, competency tags are allowlisted, and
  feedback explicitly states no compilation or execution occurred.
- **Tests executed:** All 22 frontend tests passed, including submission,
  feedback rendering, and provider-error retry. All 23 backend tests passed
  against local PostgreSQL, with the provider mocked. Frontend build/lint and
  `git diff --check` passed.
- **Remaining risks:** A real provider call was not made; configure a valid
  server-side `AI_PROVIDER_API_KEY` to test the opt-in integration. The
  progress and Learning Recall views remain future phases.

## Phase 3 — Assisted Learning Lab

- **Objective:** Deliver the first usable lesson-and-practice workspace,
  connecting Java method/input validation concepts to QA testing of a banking
  withdrawal API.
- **Implementation summary:** Added a curriculum lesson-content endpoint,
  curated explanation sections, a withdrawal-validation exercise, boundary
  cases, and a responsive lesson/practice route linked from the dashboard.
  The code editor is page-local only.
- **Trust boundary:** The draft is neither submitted nor saved, compiled, or
  evaluated. Submission and advisory AI feedback are Phase 4.
- **Tests executed:** At Phase 3, all 19 frontend tests and the complete
  19-test backend suite passed against local PostgreSQL. Frontend build/lint
  and `git diff --check` passed.
- **Remaining risks:** Other lessons are not available; no drafts, attempts, or
  feedback are persisted. V2 legacy-row backfill verification remains open.

## Phase 5 — Learning Recall

- **Objective:** Persist competency gaps from advisory reviews, offer a
  related exercise in a different context, and track an explicit review
  lifecycle without treating a single AI response as mastery.
- **Implementation summary:** Added Flyway V4 and learner-scoped competency
  recall records linked to source attempts, GET/submit/confirmation APIs, and
  a responsive recall page with transfer-exercise instructions, code
  submission, feedback, and status display. The dashboard and lesson feedback
  link into Recall.
- **Assessment policy:** `DEMONSTRATED` requires positive
  `READY_FOR_REVIEW` feedback naming the competency on both the withdrawal and
  bank-transfer exercises, followed by explicit learner confirmation. A
  single provider judgment is insufficient.
- **Tests executed:** All 26 backend tests passed against local PostgreSQL;
  all 26 frontend tests passed. Frontend production build/lint,
  diagnostics, and `git diff --check` passed. Provider calls were mocked; no
  live LLM request was made for this phase.
- **Remaining risks:** Feedback is advisory and code is not compiled or
  executed. Persisted progress reporting remains Phase 6; V2 legacy-row
  backfill verification remains open.
