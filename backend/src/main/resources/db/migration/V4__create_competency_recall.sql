CREATE TABLE learner_competency_recall (
    learner_id UUID NOT NULL REFERENCES learners(id) ON DELETE CASCADE,
    competency_tag VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    gap_attempt_id UUID NOT NULL REFERENCES learning_attempts(id) ON DELETE CASCADE,
    base_ready_attempt_id UUID REFERENCES learning_attempts(id) ON DELETE SET NULL,
    recall_ready_attempt_id UUID REFERENCES learning_attempts(id) ON DELETE SET NULL,
    learner_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (learner_id, competency_tag),
    CONSTRAINT learner_competency_recall_tag_check
        CHECK (competency_tag IN (
            'JAVA_METHODS',
            'INPUT_VALIDATION',
            'BOUNDARY_CONDITIONS',
            'DECIMAL_MONEY',
            'NULL_HANDLING'
        )),
    CONSTRAINT learner_competency_recall_status_check
        CHECK (status IN ('NEEDS_REVIEW', 'PRACTICING', 'DEMONSTRATED'))
);
