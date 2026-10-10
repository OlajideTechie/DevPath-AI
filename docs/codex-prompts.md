# Sequential Codex Prompts
Run **one prompt at a time**, review its diff and test output, then proceed. Every prompt assumes Codex reads `AGENTS.md` and relevant docs.

## 01 — Foundation
Read AGENTS.md and docs. Propose a concise plan for M1 only. Scaffold `frontend/` using React/TypeScript/Vite/Tailwind and `backend/` using Java/Spring Boot/Maven/JPA/Validation/Flyway/PostgreSQL/Actuator. Add `.gitignore`, safe `.env.example` files, development PostgreSQL Docker Compose, and local setup README. No business features yet. Verify builds and update docs/progress.md.

## 02 — Persistence
Implement Flyway V1 `learners` schema, JPA model, repository, DTOs, enums, and shared error response convention exactly per docs/architecture-and-api.md. Test PostgreSQL JSONB persistence. Do not create controllers yet. Report actual test results and update progress.

## 03 — Create learner
Implement `POST /api/v1/learners` exactly per contract. Use thin controller, service, validation, server-controlled fields, 201 Location header, and consistent 400 errors. Add happy/negative API tests. No roadmap or AI. Run tests and update progress.

## 04 — Retrieve learner
Implement `GET /api/v1/learners/{learnerId}`, 200/404/400 behaviour, API tests, minimal health endpoint, and OpenAPI docs. Verify contract and tests. Update progress.

## 05 — Landing UI
Build the responsive landing page from docs/ui-flow.md with accessible CTA, benefits, tasteful motion and reduced-motion support. No fake progress or backend calls. Verify build and update progress.

## 06 — Onboarding UI
Build the four-step wizard per docs/ui-flow.md, typed API client, inline validation, review, loading/error/success states, POST and GET integration. Prevent double submit. Add targeted frontend tests. No simulated success or roadmap. Verify build and update progress.

## 07 — Review and harden
Review M1 changes as a senior engineer: API mismatches, CORS, secrets, validation, unauthorized data exposure, error handling, migrations, accessibility, and tests. Fix critical defects only. Run builds/tests and document remaining risks.

## 08 — Backend deployment
Ask which hosting provider I prefer (Render or Railway). Prepare production Dockerfile/configuration, managed PostgreSQL env vars, health check, and restrictive CORS. Never deploy or claim success without access and verification. Document exact deployment steps and results.

## 09 — Frontend deployment
Prepare Vercel deployment with environment-based backend URL. Verify build. Document and, if access permits, run smoke tests: landing → onboarding → POST 201 → GET 200 → refresh → error state. Update progress with actual deployed URLs/results only.

## 10 — Plan M2
After M1 is verified, propose a 3–5-question diagnostic Java assessment vertical slice with server-side answer key, scoring, starting-level recommendation, UI flow, APIs, migrations, and tests. Do not implement until approved.
