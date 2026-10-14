# Architecture Decisions
- ADR-001: Monorepo (`frontend/`, `backend/`, `docs/`) for coordinated changes.
- ADR-002: Spring Boot modular monolith, not microservices.
- ADR-003: PostgreSQL + Flyway for durable and repeatable schema management.
- ADR-004: Vertical slices; M1 is onboarding only.
- ADR-005: Java is the only learning language for the POC.
- ADR-006: M1 is synthetic-data-only because authentication is not implemented.
- ADR-007: Future LLM feedback is advisory unless code is safely executed and tested.
- ADR-008: Capstone demo runs locally; cloud deployment is not a delivery
  requirement. The existing landing page and onboarding remain the foundation.
- ADR-009: Onboarding fixes current role to `QA Engineer`. Experience bands
  are persisted as categorical values; an explicit lower-bound value is used
  only to maintain the existing numeric database column for new profiles.
  Existing exact numeric values remain in the legacy column and are mapped to
  bands by Flyway V2.
- ADR-010: QA specializations are multi-valued JSONB data. Flyway V2 backfills
  the new array from the old scalar column and retains legacy data.
- ADR-011: Phase 2 recommends the first curated lesson deterministically.
  The recommendation reason uses the persisted programming level, and the QA
  example uses the persisted specialization. At that milestone the dashboard
  labels lesson activity and Learning Recall as unavailable; later phases
  replace those placeholders when workflows are implemented.
- ADR-012: Phase 3 serves curated lesson content from the curriculum module
  and keeps the editable exercise draft in page memory only. No submission,
  code persistence, compilation, or evaluation is implied before Phase 4.
- ADR-013: Phase 4 persists submitted lesson code and structured feedback in
  PostgreSQL. Feedback is advisory and never claims code execution. The
  backend uses an OpenAI-compatible endpoint configured only with server-side
  environment variables; the browser never receives provider credentials.
- ADR-014: Learning Recall persists competency gaps against source attempts
  and offers a curated transfer exercise in a different banking context.
  `DEMONSTRATED` requires positive advisory feedback identifying the same tag
  on both the original and transfer exercises plus explicit learner
  confirmation. One model response cannot establish a demonstrated status.
- ADR-015: Progress is computed from persisted learning attempts and
  competency recall records. Submission counts include failed-feedback
  attempts; latest feedback selects the newest ready result. The next activity
  prioritizes confirmation-ready evidence, then open recall work, and falls
  back to the curated starter lesson. No scores, streaks, or outcomes are
  inferred.
- ADR-016: QA example complexity is selected from the persisted QA experience
  band and examples remain relevant to the learner's specializations. Java
  focus is based on the separately declared programming level; years in QA are
  not treated as a proxy for Java proficiency.
- ADR-017: Learning Recall returns the persisted original gap-causing
  submission and advisory feedback alongside competency state so learners can
  revisit their prior work. `READY_FOR_REVIEW` remains advisory and does not
  itself mark a competency demonstrated.
- ADR-018: Evaluation findings are categorized and tied to stable competency
  identifiers. Recall is created only from explicit functional defects or
  planned reinforcement, not broad tags or optional/defensive suggestions.
  Low-confidence evaluation returns `UNCERTAIN` and creates no new gap.
- ADR-019: Feedback includes a typed next-action category. The backend rejects
  contradictory combinations of defect findings, confidence, review status,
  and next action rather than persisting misleading advice.
- ADR-020: Live provider errors remain retryable failures by default. An
  explicit local `AI_DEMO_MODE=true` selects a deterministic provider whose
  sample feedback is labeled, uncertain, and excluded from Recall evidence;
  it never activates as an automatic outage fallback.
