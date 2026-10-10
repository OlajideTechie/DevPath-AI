# Product Requirements — DevPath AI

## Problem and audience
QA engineers transitioning into Java backend engineering need more than generic roadmaps: they need teaching connected to existing QA knowledge, guided practice, useful feedback, and sustainable progress around work schedules.

## Core loop
Assess → Teach → Practice → Evaluate → Adapt → Progress.

## Milestone 1 — weekend release
- Public landing page with clear product promise and CTA.
- Four-step onboarding: QA background/skills; Java proficiency; weekly availability; review.
- POST learner profile to Spring Boot; persist in PostgreSQL.
- GET saved learner profile; show success and retrieval states.
- Responsive, accessible UI with validation, loading, and failure states.
- Deploy frontend, backend, and database; verify end-to-end with synthetic demo data.

### M1 acceptance criteria
1. Learner completes wizard and submits without full page reload.
2. Invalid inputs fail in UI and backend with clear messages.
3. Profile is saved and retrievable after server restart.
4. Deployed frontend calls deployed API over HTTPS.
5. No fake success states or claims that lessons already work.
6. Tests/builds run and results are recorded.

## Future milestones (not M1)
M2: 3–5 curated diagnostic Java questions with server-side scoring.
M3: personalized roadmap based on assessment and QA skills.
M4: interactive Java lessons, knowledge checks, and code snippet practice.
M5: AI feedback (advisory, not equivalent to code execution), Learning Pulse check-ins, progress tracking.

## Non-goals for M1
Authentication, AI calls, code execution, payments, notifications, GitHub integration, multiple languages, certificates, course marketplace.

## Product success
Eventually demonstrate one learner being taught, submitting work, receiving actionable feedback, and progressing based on evidence. M1 proves onboarding infrastructure only.
