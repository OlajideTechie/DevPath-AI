# Architecture and API Contracts

## Architecture
React + TypeScript + Vite → HTTPS REST → Spring Boot modular monolith → PostgreSQL. Later AI calls occur only from the backend. Suggested packages: `learner` (controller, dto, service, repository, model), `common` (errors, configuration); future `assessment`, `curriculum`, `learning`, `ai`, `progress`.

## M1 schema — `learners`
- `id UUID PRIMARY KEY`
- `current_role VARCHAR(100) NOT NULL`
- `years_of_experience INTEGER NOT NULL`
- `specialization VARCHAR(100) NOT NULL`
- `existing_skills JSONB NOT NULL`
- `target_role VARCHAR(100) NOT NULL` (server-fixed: `Java Backend Developer`)
- `preferred_language VARCHAR(20) NOT NULL` (server-fixed: `JAVA`)
- `programming_level VARCHAR(20) NOT NULL`
- `study_days JSONB NOT NULL`
- `daily_minutes INTEGER NOT NULL`
- `learning_style VARCHAR(30) NOT NULL`
- `profile_status VARCHAR(30) NOT NULL` (server-fixed: `CREATED`)
- `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`
Use Flyway V1; verify JSONB mapping with PostgreSQL.

## POST /api/v1/learners
Request:
```json
{"currentRole":"QA Engineer","yearsOfExperience":5,"specialization":"API Testing","existingSkills":["Postman","SQL"],"programmingLevel":"BEGINNER","studyDays":["MONDAY","WEDNESDAY","SATURDAY"],"dailyMinutes":45,"learningStyle":"PRACTICAL"}
```
Constraints: experience 0–50; daily minutes 10–240; nonempty unique study days; skills max 20, trimmed/deduplicated; bound text lengths. Programming level: BEGINNER, INTERMEDIATE, EXPERIENCED. Learning style: PRACTICAL, READING, MIXED. All days uppercase weekdays.

Success: `201 Created`, `Location: /api/v1/learners/{id}`:
```json
{"id":"8c2c1a61-25b0-4f34-9c8c-19f06db72841","currentRole":"QA Engineer","targetRole":"Java Backend Developer","preferredLanguage":"JAVA","programmingLevel":"BEGINNER","profileStatus":"CREATED","createdAt":"2026-10-10T16:30:00Z"}
```
Invalid input: 400 with envelope `{"code":"VALIDATION_ERROR","message":"Invalid learner profile","fieldErrors":{"dailyMinutes":"must be between 10 and 240"}}`.

## GET /api/v1/learners/{learnerId}
200 returns persisted fields (id, currentRole, yearsOfExperience, specialization, existingSkills, targetRole, preferredLanguage, programmingLevel, studyDays, dailyMinutes, learningStyle, profileStatus, createdAt). Unknown ID: `404 {"code":"LEARNER_NOT_FOUND","message":"Learner not found"}`. Malformed UUID: 400.

## Health
`GET /actuator/health` minimal public health status; other actuator endpoints private.

## Security boundary
No authentication in M1. Public GET-by-UUID is not appropriate for actual learner records. Only synthetic profiles may be used for public demos until authenticated ownership checks, privacy/deletion controls, and abuse protections are implemented.

## Planned API sketches (NOT M1)
- GET `/api/v1/assessments/diagnostic`
- POST `/api/v1/assessments/diagnostic/submissions`
- POST `/api/v1/learners/{learnerId}/roadmaps`
- GET `/api/v1/learners/{learnerId}/roadmaps/current`
- GET `/api/v1/lessons/{lessonId}`
- POST `/api/v1/lessons/{lessonId}/submissions`
- POST `/api/v1/learning-sessions/recommend`
- GET `/api/v1/learners/{learnerId}/progress`
Finalize future contracts before implementation.
