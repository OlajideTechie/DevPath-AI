# Capstone Progress

Current checkpoint: **Phase 6 — Persisted Progress**

## Phase status

- [x] Existing landing page retained.
- [x] Fixed QA Engineer role and experience-band selection.
- [x] Searchable multi-select QA specializations and optional skills.
- [x] Persist/retrieve updated learner contract with additive migration.
- [x] Frontend build/lint and backend compile/package verified.
- [x] Phase docs and API contracts updated.
- [x] Phase 2: dashboard, deterministic lesson recommendation, and QA context.
- [x] Frontend test suite and complete PostgreSQL-backed backend test suite.
- [ ] Verify V2 legacy-row backfill from a pre-V2 database.
- [x] Phase 3: curated lesson content and responsive practice workspace.
- [x] Phase 3 frontend and PostgreSQL-backed API tests.
- [x] Phase 4: persisted submissions and backend advisory feedback.
- [ ] Phase 4 provider integration verification with credentials (optional).
- [x] Phase 5: persisted competency gaps and related transfer exercise.
- [x] Phase 5 lifecycle, explicit evidence policy, and recall UI tests.
- [x] Phase 6: persisted progress API and dashboard summary.

## Phase 1 implementation notes

The current app is a React/TypeScript/Vite frontend and a Spring Boot modular
monolith using JPA, Bean Validation, Flyway, and PostgreSQL. The existing
landing page, four-step onboarding wizard, learner POST/GET API, and tests
remain the foundation.

Phase 1 changes the profile contract from free-form current role, numeric
experience, and one specialization to a server-fixed `QA Engineer`, an
`experienceBand`, and a list of `specializations`. Flyway V2 backfills these
new fields from existing values and retains the old database columns; new
profiles write the selected band's lower bound only to the legacy numeric
column for compatibility. Optional skills are chosen from a curated catalog.

The prior M1 docs described cloud deployment and deferred AI. The current
capstone brief supersedes that scope: the demo runs locally, and later phases
include lessons, AI feedback, Learning Recall, and persisted progress. Cloud
deployment is not required.

## Verification

Before Phase 6, frontend tests passed (26 tests across six files), and frontend
build and lint passed. The backend suite passed with 26 tests against local
PostgreSQL and the feedback provider mocked for submission tests using
`cd backend && ./mvnw --batch-mode test`. Backend package compilation and
`git diff --check` passed.
After adding LLM connection diagnostics, the focused submission controller
tests passed (4/4) against local PostgreSQL. Response parsing logs distinguish
invalid provider JSON, missing chat content, DTO mapping paths, and bean
validation failures without logging response content or learner data. Provider
instructions include an explicit JSON example requiring plain strings in all
feedback arrays. Only recognized competency-tag strings are retained if the
provider also returns malformed or unsupported tag entries; feedback fields
and the requirement for at least one valid tag remain validated. The focused
provider parsing and submission tests passed (5/5) after this adjustment.
After a provider returned truncated JSON (`JsonEOFException`), the requested
feedback was capped at three concise items per array, the response token
budget was raised to 2048, and JSON parse diagnostics were extended to include
the provider's finish reason.

The current learner, recommendation, submission, and lesson content suites
pass. A separate test applying V2 to a pre-V2 database with legacy rows has
not been run; the current database was already at the migrated schema.
Previous M1 test results predate the updated onboarding contract and do not
verify it. See
[`codex-worklog.md`](codex-worklog.md) for phase-level decisions, issues, and
remaining risks.

## Phase 2 implementation notes

The dashboard is linked from the successful profile-save state. It fetches the
saved profile and a recommended-lesson response, with loading, retryable error,
and success states. The current catalog has one starter lesson:
**Java Methods and Input Validation**. Recommendation selection is therefore
deterministic for every profile; its Java focus uses the persisted programming
level, and its banking QA example uses the persisted experience band and
specialization.

At the Phase 2 milestone, the dashboard reported the saved profile and
identified Recall as upcoming because no persisted gap workflow existed yet.
Phase 5 replaces that placeholder with the current persisted Recall status.

## Phase 3 implementation notes

The curriculum module serves the first lesson with a QA banking API context,
curated explanations for Java methods and input validation, and an exercise to
implement withdrawal validation using `BigDecimal`. Requirements and boundary
cases include null inputs, zero and negative amounts, amounts above balance,
and an amount equal to the available balance.

The responsive lab places lesson content and an editable Java draft side by
side on wide screens and stacks them on smaller screens. Drafts are page-local,
not persisted or submitted. The UI explicitly states no compilation or
evaluation occurs; submission and feedback remain Phase 4 work.

## Phase 4 implementation notes

The lesson lab submits code to a learner- and lesson-scoped backend API. The
backend validates nonblank submissions up to 20,000 characters, persists the
code and attempt status, requests structured feedback from an OpenAI-compatible
provider, validates the response and stable competency tags, then persists the
feedback as JSONB. Provider configuration is backend-only and uses environment
variables. Connection/request timeouts and a feedback-response size limit are
applied.

Provider failures leave the attempt recorded as `FEEDBACK_FAILED` and return a
controlled retryable 503 response without exposing provider details. The UI
preserves the draft and presents feedback as advisory, explicitly stating that
code was not compiled or executed and a recommendation does not establish
mastery. Backend provider logs record request start, provider HTTP status,
elapsed time, and transport/response-validation failures. Logs intentionally
exclude API keys, learner code, prompts, and provider response bodies.

## Feedback assessment accuracy update

The evaluation prompt now includes both exercise requirements and acceptance
cases. Feedback findings identify a stable competency and are categorized as
functional defects, optional improvements, additional defensive checks, or
planned reinforcement. A typed action category and confidence must agree with
the findings; inconsistent responses are rejected. Low-confidence reviews are
returned as `UNCERTAIN` with a clarification action and no findings. A
negative-available-balance guard is explicitly treated as optional for the
withdrawal exercise, not as a defect. Recall now follows only explicit
functional-defect or planned-reinforcement findings; broad competency tags
alone do not create recall entries. Flyway V5 persists the recall reason.

Regression tests cover the extra defensive balance check, missing negative
amount validation, scale-sensitive BigDecimal equality, missing null guards,
uncertain/inconsistent response handling, and gap-versus-reinforcement Recall
mapping. Legacy persisted feedback is still readable without inventing
findings. Full backend verification passed with 48 tests against local
PostgreSQL. Frontend verification passed with 28 tests; build, lint, and
`git diff --check` passed. The provider is mocked; these tests do not verify
live model behavior or Java execution.

## Phase 5 implementation notes

Flyway V4 adds a learner/competency recall record linked to the attempt that
identified the gap. Explicit `FUNCTIONAL_DEFECT` findings create
`NEEDS_REVIEW`; explicit `PLANNED_REINFORCEMENT` findings create `PRACTICING`.
Broad competency tags do not create Recall records; untouched competencies are
returned as `NOT_STARTED`. Recall presents a curated
bank-transfer validation exercise in a new context and persists its
submissions through the existing attempt/feedback flow.

The lifecycle supports `NOT_STARTED`, `NEEDS_REVIEW`, `PRACTICING`, and
`DEMONSTRATED`. The explicit policy requires `READY_FOR_REVIEW` feedback that
identifies the competency on both the original withdrawal task and the
transfer task, plus learner confirmation. A single model response cannot
establish demonstration. The Recall response now also restores the original
gap-causing code and its persisted feedback for each competency, shown as a
reference alongside the transfer task. The UI clarifies that
`READY_FOR_REVIEW` is positive advisory feedback, not verified correctness.
The dashboard and lesson feedback link into the Recall page, which shows
current status, exercise, retry/error handling, and the confirmation action.

## Phase 5 verification

The full backend suite passed: 26 tests against local PostgreSQL. Recall
controller tests cover gap persistence, lifecycle transitions, validation,
the evidence gate, and learner confirmation. All 26 frontend tests passed;
frontend build and lint passed. `git diff --check` passed. Feedback-provider
tests use mocks; no live provider request was made for Phase 5.

## Recall submission review update

Recall now returns the saved original gap-causing submission and its advisory
feedback for each persisted competency. The screen presents that original work
as an accessible, expandable reference beside the transfer task. A matching
`READY_FOR_REVIEW` transfer response is described as positive advisory feedback
only; it does not establish correctness or demonstration by itself.

Verification for this update: the full backend suite passed with 39 tests
against local PostgreSQL; the focused Recall controller tests passed (2 tests).
The full frontend suite passed with 28 tests, and `npm run build`,
`npm run lint`, and `git diff --check` passed. No live LLM provider request was
made.

## Phase 6 implementation notes

`GET /api/v1/learners/{learnerId}/progress` derives distinct lessons attempted,
all persisted exercise submissions, persisted competency states, and the
latest ready feedback from PostgreSQL. A failed provider request still counts
as a saved submission, but is not presented as feedback. The dashboard now
shows the review/in-practice/demonstrated competency groups, latest available
feedback, and a linked next activity. No scores, streaks, or code execution
claims are generated.

Next activity selection is deterministic: first resolve a positive evidence
pair awaiting learner confirmation, then address a `NEEDS_REVIEW` competency,
then continue incomplete transfer evidence (returning to the original lesson
if its positive review is missing); otherwise use the curated starter lesson.
The endpoint is read-only and does not call the LLM provider.

## Experience-tailored examples

Recommended QA scenarios now scale from straightforward examples for the
lowest experience bands to layered regression and contract coverage for the
highest bands, while reflecting selected QA specializations. The same scenario
appears in both the dashboard and lesson workspace. Java focus remains tied
to the separately declared programming level. Backend coverage checks all
five experience bands and all three programming levels; frontend coverage
checks both personalized context blocks in the lesson workspace.

Verification after this change: `cd backend && set -a && source .env && set +a
&& ./mvnw --batch-mode test` passed (39 tests, 0 failures/errors) against local
PostgreSQL. `cd frontend && npm test` passed (28 tests); `npm run build` and
`npm run lint` passed. `git diff --check` passed.

## Phase 6 verification

The full backend suite passed with 31 tests against local PostgreSQL, including
progress for empty profiles, persisted counts and competency statuses, latest
feedback, deterministic next activity, provider-failure accounting, unknown
learners, malformed IDs, and OpenAPI documentation. The full frontend suite
passed with 28 tests; `npm run build` and `npm run lint` passed. An initial
frontend test run failed one navigation test while the full suite, build, and
lint were running concurrently; that test and the subsequent full suite passed
when rerun without the concurrent build. `git diff --check` passed. Maven
emitted 401 warnings while checking optional repository metadata, but resolved
dependencies and completed the test build successfully.

## Historical M1 verification

Before the capstone profile-contract update, the recorded M1 results were 10
frontend tests passing, frontend build/lint passing, and 14 backend tests
passing against local PostgreSQL. Those results describe the earlier contract;
they are retained as history and are not evidence for the current Phase 1
changes.

## Remaining risks

- Learner API request/response fields changed while retaining the `/api/v1`
  path. This is suitable for the local capstone but would require a compatibility
  strategy for deployed external clients.
- The capstone remains synthetic-data-only without authentication or ownership
  checks.
- Real provider behavior remains an opt-in integration check with valid
  credentials.

## Explicit demo feedback mode

Added `AI_DEMO_MODE=false` as the default backend setting. Setting it to
`true` selects a deterministic sample provider instead of the live provider;
provider failures never trigger sample feedback automatically. The mock result
is tagged `DEMO_SAMPLE`, clearly labeled in lesson, Recall, and progress
surfaces, and always uncertain with no findings, so it cannot create Recall
gaps or imply that submitted code was analyzed. The feedback source is persisted
with the attempt and returned by the progress API.

Verification: the full backend suite passed (50 tests, 0 failures/errors) with
local PostgreSQL. The frontend suite passed (30 tests); build and lint passed.
`git diff --check` passed. No live provider request or submitted Java execution
was performed.
