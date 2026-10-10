# DevPath AI — Codex Instructions

## Mission
Build an adaptive learning companion for QA engineers transitioning into Java backend engineering. The product should assess, teach, provide practice, evaluate, adapt to available time and energy, and track demonstrated progress. The first release is a deployed onboarding vertical slice, NOT the full learning experience.

## Read before changing code
- `docs/product-requirements.md` — scope and acceptance criteria
- `docs/architecture-and-api.md` — architecture, database, and API contracts
- `docs/ui-flow.md` — screens and interaction rules
- `docs/progress.md` — current milestone
- `docs/decisions.md` — technical decisions
Flag conflicting requirements before coding. Do not invent new features.

## Stack and architecture
- `frontend/`: React, TypeScript, Vite, Tailwind CSS.
- `backend/`: Java, Spring Boot, Maven, Spring Data JPA, Bean Validation, Flyway, PostgreSQL.
- Modular monolith; REST/JSON; no microservices or queues for MVP.
- Do not add AI integration, login, or future milestones to the first release.

## Implementation rules
- Work in small, reviewable vertical slices; inspect the code before editing.
- Explain a brief plan before substantial changes.
- Keep controllers thin, business rules in services, persistence in repositories.
- Use DTOs; do not expose JPA entities in API responses.
- Use constructor injection, server-side validation, centralized error handling, and Flyway migrations.
- Keep frontend types consistent with API contracts.
- Use accessible, responsive UI with real loading, error, success, and empty states.
- Respect reduced-motion preferences; use restrained animation.
- No secrets in code, commits, logs, or browser bundles.
- Do not perform unrelated refactors or claim tests passed without running them.

## Security
The initial POC has NO authentication. UUIDs are not access control. Use synthetic demo profiles only in public deployment. Do not collect names, email addresses, or other unnecessary personal details. Real users require authentication, ownership checks, privacy controls, and abuse prevention. Restrict CORS, validate inputs, and do not expose privileged operational endpoints.

## Testing and review
- Backend: tests for happy path, invalid input, and missing resources; test migrations with PostgreSQL.
- Frontend: test form validation, submission, API failures, and retry behaviour.
- Review every change for correctness, security, data integrity, API compatibility, maintainability, and deployment risks.
- Before completing work, run applicable builds/tests, record actual outcomes, and update `docs/progress.md`.

## Task completion report
Report: (1) changes made, (2) files changed, (3) commands/tests run and outcomes, (4) remaining risks, (5) next small task.

`AGENTS.md` instructs Codex; it does NOT automatically log activity. Use Git/PRs for code history, `docs/progress.md` for milestones, and `docs/decisions.md` for architectural decisions.
