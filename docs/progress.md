# Progress — M1 Onboarding
Status: In progress.
- [x] Scaffold monorepo and tools
- [x] Flyway learner migration and repository
- [x] POST learner API with validation/tests
- [x] GET learner API with tests
- [x] Landing page
- [x] Onboarding wizard and API integration
- [ ] Deploy backend with PostgreSQL
- [ ] Deploy frontend and smoke-test full flow

Deployment is pending Render service configuration and the GitHub Actions
`RENDER_DEPLOY_HOOK_URL` secret.

Test and build evidence: `cd backend && ./mvnw test` passed against local
PostgreSQL (14 tests, 0 failures/errors). Tests cover Flyway/JPA context startup,
JSONB persistence, minimal public health and private non-health Actuator routes,
Swagger UI/OpenAPI spec, POST/GET learner contracts, server-controlled fields,
skill normalization, validation errors, malformed UUIDs, and missing learners.
Frontend evidence: `cd frontend && npm run build` and `npm run lint` passed.
The landing page has responsive layouts, keyboard skip navigation and visible
focus states, reduced-motion support, the specified CTA and benefits, and demo
labeling. `cd frontend && npm test` passed (10 tests) for onboarding
validation/review, POST/GET integration behavior, failure/retry, and duplicate
submission prevention. The four-step onboarding flow is implemented with
typed API requests and saved-profile retrieval. Backend verification after
restricting API CORS to configured frontend origins, including both local
`localhost` and `127.0.0.1`: `cd backend && set -a && source .env && set +a
&& ./mvnw test` passed (14 tests, 0 failures/errors).
`git diff --check` passed. CI/deployment automation was added in
`.github/workflows/ci-cd.yml`: pull requests and main-branch changes run both
test suites, backend tests use an ephemeral PostgreSQL 17 service, and the
Render deploy hook runs only after both test jobs pass on `main`. The workflow
does not run separate lint/build jobs; Render handles its service build during
deployment. CI test-database username and password are configurable through the
`CI_POSTGRES_USER` Actions variable and `CI_POSTGRES_PASSWORD` Actions secret;
safe defaults apply only to the temporary CI database. Local verification for
this workflow change: frontend tests passed
(10 tests), frontend build and lint passed, and backend tests passed
(14 tests, 0 failures/errors) against local PostgreSQL. Workflow YAML parsing
and `git diff --check` passed. Render deployment has not yet been run.
