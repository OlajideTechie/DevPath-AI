# DevPath AI

**Turn your QA experience into a foundation for Java backend engineering.**

DevPath AI is an onboarding prototype for QA engineers who want to move into
Java backend development. Instead of treating a learner as a blank slate, the
product starts by understanding their testing background, existing skills,
Java proficiency, and available study time.

This hackathon release delivers the first vertical slice: a landing page, a
guided learner-profile wizard, and a backend that validates, stores, and
returns that profile. It demonstrates the foundation for a more personal
learning journey; it does **not** yet teach lessons, assess coding ability, or
generate a roadmap.

## Why it matters

Many career-transition resources offer generic course lists without connecting
new material to what a learner already knows or the time they can realistically
commit. QA engineers bring valuable foundations—such as API testing, SQL,
debugging, and systematic investigation—that can help make a transition into
backend development more relevant and achievable.

DevPath AI is designed to make that starting point explicit:

- **Recognize transferable skills:** capture a learner's QA role,
  specialization, experience, and existing skills.
- **Make the next step personal:** capture Java proficiency, study days,
  available minutes, and learning preferences before designing future learning.
- **Build trust with real persistence:** save the submitted profile in
  PostgreSQL and retrieve it from the API rather than showing a simulated
  success.
- **Create a foundation for evidence-based progress:** future learning can
  build on learner context and demonstrated work, rather than a one-size-fits-
  all roadmap.

The intended impact is a more grounded transition experience: learners can
start from their actual strengths and constraints, and a future product can
adapt instruction as they demonstrate progress. This release proves the
onboarding and persistence foundation—not that learning outcomes have already
been measured.

## What is implemented

- Responsive landing page and four-step onboarding experience.
- Typed frontend API client for learner profile creation and retrieval.
- `POST /api/v1/learners` with request validation, normalized skills,
  server-controlled role/language/status, and a `201 Created` response with a
  `Location` header.
- `GET /api/v1/learners/{learnerId}` with persisted profile data and consistent
  not-found and invalid-ID errors.
- PostgreSQL learner storage with JSONB fields managed by a Flyway migration.
- Minimal public health check at `GET /actuator/health`.
- OpenAPI specification and interactive Swagger UI.
- Configurable CORS for explicitly allowed frontend origins.
- Frontend and backend tests covering core UI, API, validation, and persistence
  behavior.

## What is not implemented

This is an **M1 onboarding prototype**, not a complete learning platform. It
does not include authentication, diagnostic assessments, generated roadmaps,
lessons, code execution, AI features, or progress tracking. Those are possible
future milestones and are intentionally not represented as working features.

## Repository structure

```text
.
├── backend/
│   ├── src/main/java/ai/devpath/
│   │   ├── common/                 # Shared CORS configuration and API errors
│   │   └── learner/                # Learner controller, DTOs, service, model,
│   │                               # repository, and profile enums
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
│   │   ├── assets/                 # Static visual assets
│   │   ├── App.tsx                 # Routes and landing page
│   │   └── index.css               # Global styles and Tailwind entry point
│   ├── .env.example                # Frontend API URL template
│   ├── package.json                # Scripts and dependencies
│   └── vite.config.ts              # Vite and frontend test configuration
├── docs/
│   ├── product-requirements.md     # M1 scope, acceptance criteria, future scope
│   ├── architecture-and-api.md     # Data model and API contracts
│   ├── ui-flow.md                  # Screen and interaction requirements
│   ├── decisions.md                # Technical and product decisions
│   ├── codex-prompts.md            # Reusable project prompts
│   └── progress.md                 # Milestone status and verification record
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
- **`docs/` keeps scope honest:** requirements and decisions distinguish
  working M1 behavior from ideas reserved for later milestones.

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

The frontend communicates with the backend over HTTP in local development. The
backend owns validation and server-controlled profile fields. PostgreSQL stores
the profile, including JSONB skill and study-day data. There are no
microservices, queues, or AI calls in this release.

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
environment. For a deployed frontend, configure `VITE_API_BASE_URL` with the
reachable HTTPS API origin, configure `FRONTEND_ORIGINS` with the deployed
frontend origin, and rebuild/restart as appropriate.

## API and operational endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/learners` | Validate and save a learner profile; returns `201` and a `Location` header |
| `GET` | `/api/v1/learners/{learnerId}` | Retrieve a saved learner profile |
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
    "currentRole": "QA Engineer",
    "yearsOfExperience": 5,
    "specialization": "API Testing",
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
For deployment, use managed secrets, HTTPS, and a narrowly configured
`FRONTEND_ORIGINS` allowlist.
