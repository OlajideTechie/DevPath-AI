CREATE TABLE learning_attempts (
    id UUID PRIMARY KEY,
    learner_id UUID NOT NULL REFERENCES learners(id) ON DELETE CASCADE,
    lesson_id VARCHAR(100) NOT NULL,
    submitted_code TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    feedback_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT learning_attempts_status_check
        CHECK (status IN ('FEEDBACK_READY', 'FEEDBACK_FAILED'))
);

CREATE INDEX learning_attempts_learner_created_idx
    ON learning_attempts (learner_id, created_at DESC);
