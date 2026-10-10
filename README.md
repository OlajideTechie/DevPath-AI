# DevPath AI

M1 project scaffold for the DevPath AI onboarding release. This repository
contains the learner profile API and onboarding flow. Learning, assessment,
roadmap, and AI features have not been implemented.

## Prerequisites

- Node.js 22.12+ and npm
- Java 21
- PostgreSQL 17

## Start PostgreSQL

Install and start PostgreSQL using your operating system's package manager or
service manager. Create a local database and role using a PostgreSQL
superuser:

```sh
psql postgres
```

Then run these SQL statements in the `psql` prompt:

```sql
CREATE ROLE devpath LOGIN PASSWORD 'devpath_local_only';
CREATE DATABASE devpath OWNER devpath;
```

The sample credentials are for local development only; do not use them in
shared or deployed environments. If you choose different credentials, update
`backend/.env` to match.

## Run the backend

```sh
cd backend
cp .env.example .env
set -a
source .env
set +a
./mvnw spring-boot:run
```

The backend connects to PostgreSQL on `localhost:5432` and serves the learner
API at `/api/v1/learners`. Its minimal health endpoint is
<http://localhost:8080/actuator/health>.
API CORS is restricted to the origins configured by `FRONTEND_ORIGINS` in
`backend/.env` (default local origins: `http://localhost:5173` and
`http://127.0.0.1:5173`). Set `FRONTEND_ORIGINS` to the exact frontend origin
when deploying.

Interactive API documentation is available at
<http://localhost:8080/swagger-ui/index.html>; the OpenAPI JSON is at
<http://localhost:8080/v3/api-docs>. Swagger documents the learner endpoints.
Actuator exposes only the health endpoint; its health response omits component
details.

## Run the frontend

In a second terminal:

```sh
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Vite prints the local development URL after startup. The onboarding flow sends
learner requests to the API base URL in `frontend/.env.local`; the example
points to `http://localhost:8080`.

## Build and test

```sh
(cd frontend && npm run build && npm test && npm run lint)
(cd backend && ./mvnw -DskipTests package)
```

Run backend tests with PostgreSQL available and the backend environment loaded:

```sh
cd backend
set -a
source .env
set +a
./mvnw test
```

Stop PostgreSQL using the service manager used to start it.
