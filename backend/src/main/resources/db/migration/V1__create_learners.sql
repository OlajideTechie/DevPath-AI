CREATE TABLE learners (
    id UUID PRIMARY KEY,
    "current_role" VARCHAR(100) NOT NULL,
    years_of_experience INTEGER NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    existing_skills JSONB NOT NULL,
    target_role VARCHAR(100) NOT NULL,
    preferred_language VARCHAR(20) NOT NULL,
    programming_level VARCHAR(20) NOT NULL,
    study_days JSONB NOT NULL,
    daily_minutes INTEGER NOT NULL,
    learning_style VARCHAR(30) NOT NULL,
    profile_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
