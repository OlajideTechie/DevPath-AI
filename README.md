# DevPath AI

**Turn your QA experience into a foundation for Java backend engineering.**

DevPath AI is a local learning prototype for QA engineers who want to move
into Java backend development. It connects a learner's QA background and
schedule to a curated Java practice activity, then records submissions,
advisory feedback, competency review states, and progress. It starts by
understanding what the learner already knows rather than treating them as a
blank slate.

The capstone extends an existing learner-onboarding application toward a
complete local learning loop: understand the learner, teach, practice,
evaluate, reinforce, and track progress. The local application includes the
onboarding, curated Java lesson, advisory feedback, Learning Recall, and
persisted progress phases.

## Why it matters

Many career-transition resources offer generic course lists without connecting
new material to what a learner already knows or the time they can realistically
commit. QA engineers bring valuable foundations—such as API testing, SQL,
debugging, and systematic investigation—that can help make a transition into
backend development more relevant and achievable.

DevPath AI is designed to make that starting point explicit:

- **Recognize transferable skills:** capture a learner's QA role,
  specialization, experience, and existing skills.
- **Make the next step personal:** use Java proficiency, study days, available
  minutes, and learning preferences to contextualize the starting lesson;
  adapt QA examples from straightforward cases to more layered coverage based
  on QA experience.
- **Build trust with real persistence:** save the submitted profile in
  PostgreSQL and retrieve it from the API rather than showing a simulated
  success.
- **Track evidence-based progress:** report persisted lesson attempts,
  submissions, competency review states, latest feedback, and a deterministic
  next activity rather than invented scores or streaks.

The intended impact is a more grounded transition experience: learners can
start from their actual strengths and constraints, and a future product can
adapt instruction as they demonstrate progress. This release proves the
goal is a relevant, evidence-based learning journey. Do not claim measured
learning outcomes until the application has persisted evidence for them.

## What is implemented

- Responsive landing page and four-step onboarding experience.
- Typed frontend API client for learner profile creation and retrieval.
- `POST /api/v1/learners` with request validation, normalized skills,
  server-controlled role/language/status, and a `201 Created` response with a
  `Location` header.
- QA Engineer role fixed by the backend; categorical experience bands and
  multi-select specialization data migrated additively from the earlier
  profile schema.
- `GET /api/v1/learners/{learnerId}` with persisted profile data and consistent
  not-found and invalid-ID errors.
- PostgreSQL learner storage with JSONB fields managed by a Flyway migration.
- Deterministic lesson recommendation, curated Java practice, persisted
  advisory submissions, and targeted cross-context Learning Recall.
- Persisted progress summary with lesson/submission counts, competency states,
  latest available feedback, and an evidence-based next activity.
- Minimal public health check at `GET /actuator/health`.
- OpenAPI specification and interactive Swagger UI.
- Configurable CORS for explicitly allowed frontend origins.
- Frontend and backend tests covering core UI, API, validation, and persistence
  behavior.

## Scope and limitations

Additional learning tracks and future product expansion are not included.
Code feedback is advisory: submissions are not compiled or executed, and
feedback must not be presented as evidence that code passed tests. The progress
summary reports persisted activity only; it does not claim measured learning
outcomes. `READY_FOR_REVIEW` is a positive advisory recommendation, not proof
of correctness. Demonstrated status also requires positive advisory feedback
on both exercises and the learner's explicit confirmation.

This modular monolith and PostgreSQL setup are suitable for a local hackathon
proof of concept, not a production-scale service. The current release has one
curated lesson, synchronous LLM feedback, and no authentication or learner
ownership enforcement. Automated tests mock the feedback provider; configure
and verify a real provider separately before relying on live feedback in a
demo.

## Repository structure

```text
.
├── backend/
│   ├── src/main/java/ai/devpath/
│   │   ├── common/                 # Shared CORS configuration and API errors
│   │   ├── learner/                # Learner controller, DTOs, service, model,
│   │   │                           # repository, and profile enums
│   │   ├── curriculum/             # Curated starter lesson and recommendation
│   │   ├── feedback/               # Persisted code attempts and advisory feedback
│   │   ├── recall/                 # Persisted competency gaps and transfer practice
│   │   └── progress/               # Persisted progress summary and next activity
│   ├── src/main/resources/
│   │   ├── db/migration/           # Versioned PostgreSQL schema (Flyway)
│   │   └── application.properties  # Database, server, health, and CORS config
│   ├── src/test/java/              # API, repository, health, and OpenAPI tests
│   ├── .env.example                # Safe local configuration template
│   ├── mvnw                        # Maven wrapper
│   └── pom.xml                     # Spring Boot dependencies and build
├── frontend/
│   ├── src/
│   │   ├── api/                    # Typed learner API client and tests
│   │   ├── onboarding/             # Wizard UI, styles, and interaction tests
│   │   ├── dashboard/              # Learner dashboard, lesson recommendation
│   │   ├── lesson/                 # Responsive lesson-and-practice workspace
│   │   ├── recall/                 # Competency review and transfer exercise
│   │   ├── assets/                 # Static visual assets
│   │   ├── App.tsx                 # Routes and landing page
│   │   └── index.css               # Global styles and Tailwind entry point
│   ├── .env.example                # Frontend API URL template
│   ├── package.json                # Scripts and dependencies
│   └── vite.config.ts              # Vite and frontend test configuration
├── docs/
│   ├── product-requirements.md     # Capstone goal, phases, boundaries
│   ├── architecture-and-api.md     # Data model and API contracts
│   ├── ui-flow.md                  # Screen and interaction requirements
│   ├── decisions.md                # Technical and product decisions
│   ├── codex-prompts.md            # Historical implementation prompts
│   ├── api-contracts.md            # Current API contracts and response examples
│   ├── codex-worklog.md            # Verified phase worklog
│   └── progress.md                 # Capstone phase status and test evidence
├── .gitignore
└── README.md
```

### How the structure creates value

- **`frontend/` keeps the demo understandable:** presentation, onboarding
  interactions, and API communication are separated so the user flow can
  evolve without coupling every screen to persistence details.
- **`backend/` protects the data contract:** controllers handle HTTP, services
  apply profile rules, repositories persist records, and DTOs define the API
  rather than exposing database entities.
- **Flyway migrations make storage repeatable:** the learner schema is
  version-controlled and can be applied consistently across environments.
- **`docs/` keeps scope honest:** requirements, API contracts, and decisions
  describe implemented behavior and identify remaining follow-up work.

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

The frontend communicates with the backend over HTTP locally. The backend
owns validation and server-controlled profile fields. PostgreSQL stores the
profile, including JSONB arrays. Curated lessons stay separate from backend
application logic; any LLM provider call belongs on the backend only.

## Run locally

### Requirements

- Node.js 22.12 or newer and npm
- Java 21
- PostgreSQL 17, running locally

This setup uses a locally installed PostgreSQL service; Docker is not required.

### 1. Create a local database

Connect to PostgreSQL as a local administrator:

```sh
psql postgres
```

Create a role and database for development:

```sql
CREATE ROLE devpath LOGIN PASSWORD 'devpath_local_only';
CREATE DATABASE devpath OWNER devpath;
```

These credentials are examples for a local machine only. Never reuse them for
a shared, public, or production environment. If you choose different values,
update the matching variables in `backend/.env`.

To enable real advisory feedback, add your provider key as
`AI_PROVIDER_API_KEY` in `backend/.env` only. The backend uses an
OpenAI-compatible chat completions endpoint by default; configure
`AI_PROVIDER_BASE_URL` and `AI_PROVIDER_MODEL` there if needed. Without a key,
lesson loading still works, while submissions return a retryable unavailable
response and are recorded as failed feedback attempts. Never add provider
credentials to frontend environment variables.

For a screen-recorded demo when the provider is unavailable, set
`AI_DEMO_MODE=true` in `backend/.env` and restart the backend. This opt-in mode
returns a clearly labeled sample response, not an assessment of the submitted
code. It is marked uncertain, creates no competency gaps, and never activates
automatically when a live provider fails. Set it back to `false` to use the
provider again.

### 2. Start the backend

In a terminal:

```sh
cd backend
cp .env.example .env
set -a
source .env
set +a
./mvnw spring-boot:run
```

The first start applies the Flyway migration. The API listens on
`http://localhost:8080` by default.
LLM connection diagnostics (request start, HTTP status, duration, response
shape/schema failure details, and transport failure category) are printed in
this backend terminal. API keys, prompts, learner code, and provider response
bodies are not logged.

For a managed PostgreSQL provider such as Neon, set `SPRING_DATASOURCE_URL` in
`backend/.env` to a JDBC URL beginning with `jdbc:postgresql://`, using the
provider's host and database. For example:

```dotenv
SPRING_DATASOURCE_URL="jdbc:postgresql://<host>/<database>?sslmode=require"
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<database-password>
```

Do not paste a provider URI beginning with `postgres://` or `postgresql://`
directly into `SPRING_DATASOURCE_URL`; Spring expects the JDBC prefix. Keep
username and password in their corresponding variables. Quote the URL if its
query string contains shell-special characters, then reload `backend/.env`
before starting the application.

### 3. Start the frontend

In another terminal:

```sh
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Open the local URL printed by Vite (normally
`http://localhost:5173`). The example `VITE_API_BASE_URL` points to
`http://localhost:8080`.

The backend's `FRONTEND_ORIGINS` setting must list the exact browser origin
used for the frontend. The local example allows both `http://localhost:5173`
and `http://127.0.0.1:5173`. Restart the backend after changing its
environment. Cloud deployment is not required for the capstone.

## API and operational endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/learners` | Validate and save a learner profile; returns `201` and a `Location` header |
| `GET` | `/api/v1/learners/{learnerId}` | Retrieve a saved learner profile |
| `GET` | `/api/v1/learners/{learnerId}/recommended-lesson` | Get the personalized starting-lesson recommendation |
| `GET` | `/api/v1/lessons/{lessonId}` | Get curated lesson content and its exercise |
| `POST` | `/api/v1/learners/{learnerId}/lessons/{lessonId}/submissions` | Persist a code attempt and return advisory feedback |
| `GET` | `/api/v1/learners/{learnerId}/recall` | Get persisted competency status and the transfer exercise |
| `POST` | `/api/v1/learners/{learnerId}/recall/submissions` | Submit the transfer exercise for advisory feedback |
| `POST` | `/api/v1/learners/{learnerId}/recall/{competencyTag}/demonstration` | Confirm competency after the assessment policy is met |
| `GET` | `/api/v1/learners/{learnerId}/progress` | Get persisted activity, competency states, latest feedback, and next activity |
| `GET` | `/actuator/health` | Minimal service health status |
| `GET` | `/swagger-ui/index.html` | Interactive API documentation |
| `GET` | `/v3/api-docs` | OpenAPI JSON document |

When the backend is running locally, Swagger UI is available at
<http://localhost:8080/swagger-ui/index.html>, and the health check is at
<http://localhost:8080/actuator/health>. Only the health endpoint is exposed
from Actuator; component details are hidden.

Example profile creation:

```sh
curl -i http://localhost:8080/api/v1/learners \
  -H 'Content-Type: application/json' \
  -d '{
    "experienceBand": "FOUR_TO_FIVE",
    "specializations": ["API Testing", "Database Testing"],
    "existingSkills": ["Postman", "SQL"],
    "programmingLevel": "BEGINNER",
    "studyDays": ["MONDAY", "WEDNESDAY", "SATURDAY"],
    "dailyMinutes": 45,
    "learningStyle": "PRACTICAL"
  }'
```

The successful response includes an `id`. Use that ID to retrieve the saved
profile:

```sh
curl http://localhost:8080/api/v1/learners/<learner-id>
```

Invalid profile input returns HTTP 400 with field-level errors. A valid UUID
that does not identify a saved profile returns HTTP 404.

## Build and test

Run the frontend checks:

```sh
cd frontend
npm test
npm run build
npm run lint
```

Run backend tests with PostgreSQL available and the backend environment
loaded:

```sh
cd backend
set -a
source .env
set +a
./mvnw test
```

The backend test suite exercises PostgreSQL-backed application startup and
JSONB persistence in addition to API validation and error behavior. The
current milestone's recorded results are in [`docs/progress.md`](docs/progress.md).

## Security and demo data

Authentication and ownership checks are **not implemented**. A UUID is an
identifier, not access control, and the profile retrieval endpoint is not
suitable for private real-user data. Use synthetic demo profiles only. Do not
collect personal details or expose real learner records in a public demo.

Environment files containing local configuration are ignored by Git; commit
only the `.env.example` templates. Before using real users, add authentication,
ownership enforcement, privacy and deletion controls, and abuse protection.

## Capstone execution

The capstone is designed to run locally for a screen-recorded demo; cloud
deployment is not required. The existing GitHub workflow and deployment
configuration came from the earlier M1 setup and are not part of this local
demo setup. Start the frontend, Spring Boot backend, and PostgreSQL using the
local instructions above.

The implemented learner journey includes onboarding, a recommended curated
Java lesson, persisted exercise submissions, backend-only advisory LLM
feedback, Learning Recall with the original gap-causing submission available
for review, and persisted progress reporting. The learner role is
server-controlled as QA Engineer; the profile API accepts an `experienceBand`
and an array of `specializations`. See
[`docs/api-contracts.md`](docs/api-contracts.md) for current request and
response contracts.

Configure `AI_PROVIDER_API_KEY` only in `backend/.env` to request live
feedback. Automated tests mock the provider, so verify the configured provider
connection and response before a demo that depends on it. If it is unavailable,
submission feedback returns a retryable error and no successful feedback is
simulated.
