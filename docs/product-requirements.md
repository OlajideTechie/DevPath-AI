# Product Requirements — DevPath AI Capstone

## Product goal

DevPath AI helps QA engineers transition into Java backend development through
contextual teaching, hands-on practice, AI-assisted feedback, targeted
reinforcement, and evidence-based progress.

The core loop is:

**Understand the learner → Teach → Practice → Evaluate → Identify gaps →
Reinforce → Track progress.**

The signature feature is **Learning Recall**: when a learner struggles with a
concept, record the competency gap and offer a related exercise in a different
context.

## Capstone delivery

The application runs locally for a screen-recorded demonstration. Cloud
deployment is not required. Preserve the existing landing page and onboarding
flow, and extend the current React/TypeScript frontend and Spring Boot
modular-monolith backend. Use the configured PostgreSQL database and keep LLM
provider credentials on the backend.

### Phase 1 — QA-focused onboarding

- Keep the current landing page and four-step review flow.
- Fix current role to `QA Engineer`; do not ask learners to type it.
- Select experience from `0–1`, `2–3`, `4–5`, `6–8`, or `9+` years.
- Select at least one QA specialization from the curated catalog.
- Allow optional technical skills from the curated catalog.
- Review and edit answers before submitting; save and retrieve the real profile.
- Preserve a responsive, keyboard-accessible UI with useful validation,
  loading, success, and error states.
- Store a defined experience-band value; preserve and migrate existing profile
  data rather than silently interpreting a band as an exact number of years.

### Phase 2 — Personalized learning entry

- After profile creation, provide a lightweight dashboard with learner context,
  a recommended lesson, the current milestone, activity status, and Learning
  Recall state.
- Determine a starting lesson with transparent, deterministic rules.
- Use curated lesson content; use QA experience band to scale the complexity
  of QA examples, and use QA specialization to make those examples relevant.
- Keep Java concept depth tied to the learner's declared programming level;
  QA tenure must not be treated as a proxy for Java proficiency.
- Show only persisted or rule-derived information. Do not invent completion,
  mastery, or progress percentages.

### Phase 3 — Assisted Learning Lab

- Provide a responsive lesson-and-practice workspace.
- First lesson: **Java Methods and Input Validation**.
- Connect the lesson to QA testing of banking APIs.
- Exercise: implement withdrawal validation for available balance, requested
  amount, non-positive values, and appropriate monetary types.
- Clearly label any unimplemented content as upcoming.

### Phase 4 — AI-assisted feedback

- Send code only from the backend to the configured LLM provider.
- Validate submission size and lesson context; apply provider timeouts and
  controlled error handling.
- Review code against explicit exercise requirements and acceptance cases;
  distinguish functional defects, optional improvements, additional defensive
  checks, and planned reinforcement with competency identifiers and confidence.
- Request and validate structured advisory feedback: summary, findings,
  strengths, issues, improvements, competency tags, next action, confidence,
  and review recommendation.
- Do not count harmless extra validation as a defect. Missing requirements
  need targeted corrections; satisfied requirements need reinforcement or a
  next lesson; uncertain assessments must state uncertainty.
- Persist attempts and feedback.
- Mock provider calls in automated tests; test real integration separately
  when credentials are configured.
- Permit an explicitly enabled local demo provider during provider downtime
  only if its sample feedback is clearly labeled, uncertain, and excluded from
  competency evidence; never switch to mock feedback automatically.
- Do not claim code has compiled or passed tests unless it was actually run by
  an execution environment.

### Phase 5 — Learning Recall

- Persist competency gaps and associate them with attempts and exercises.
- Create Recall recommendations only from explicit functional-defect findings
  or deliberate planned-reinforcement findings, never from unclassified tags.
- Provide a curated related follow-up exercise (for example, a bank transfer
  after a withdrawal-validation exercise).
- During Recall, let the learner reopen the saved code and advisory feedback
  from the original submission that identified the selected competency gap.
- Let the learner submit and receive feedback on the recall exercise.
- Support `NOT_STARTED`, `NEEDS_REVIEW`, `PRACTICING`, and `DEMONSTRATED`.
- Use an explicit assessment policy; one LLM judgment alone cannot establish
  mastery. Do not build a complex spaced-repetition engine.
- Treat `READY_FOR_REVIEW` as positive advisory feedback, not proof of
  correctness. `DEMONSTRATED` still requires positive feedback identifying
  the competency on both contexts and explicit learner confirmation.

### Phase 6 — Progress

Show persisted facts: lessons attempted, exercises submitted, competencies
needing review, competencies demonstrated, latest feedback, and the
recommended next activity. Do not fabricate scores, streaks, or outcomes.

## Definition of done

A learner can open the existing landing page, complete and save the improved
onboarding, reach a recommended Java lesson, study a QA-contextual explanation,
submit an exercise, receive actual advisory AI feedback, see a persisted gap,
attempt a targeted recall exercise, and view updated persisted progress. The
full experience runs locally and is suitable for a screen recording.

## Non-goals

- Cloud deployment, authentication, registration, or ownership controls.
- Payments, notifications, or additional learning tracks.
- Microservices, complex AI agents, or a code-execution platform.
- Claims that AI-reviewed submissions were compiled or executed.
- Fabricated success, feedback, or progress.

## Geographic scalability

Avoid country-specific business rules. Keep lesson content separate from
application logic, use stable competency identifiers, store timestamps
consistently, and avoid region-specific progression assumptions. Localization
and expanded curricula are future directions, not required infrastructure for
this capstone.
