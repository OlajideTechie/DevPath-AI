ALTER TABLE learner_competency_recall
    ADD COLUMN reason VARCHAR(30) NOT NULL DEFAULT 'COMPETENCY_GAP';

ALTER TABLE learner_competency_recall
    ADD CONSTRAINT learner_competency_recall_reason_check
        CHECK (reason IN ('COMPETENCY_GAP', 'PLANNED_REINFORCEMENT'));
