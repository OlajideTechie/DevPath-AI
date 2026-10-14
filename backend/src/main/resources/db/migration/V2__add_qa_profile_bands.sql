ALTER TABLE learners
    ADD COLUMN experience_band VARCHAR(20),
    ADD COLUMN specializations JSONB;

UPDATE learners
SET experience_band = CASE
        WHEN years_of_experience BETWEEN 0 AND 1 THEN 'ZERO_TO_ONE'
        WHEN years_of_experience BETWEEN 2 AND 3 THEN 'TWO_TO_THREE'
        WHEN years_of_experience BETWEEN 4 AND 5 THEN 'FOUR_TO_FIVE'
        WHEN years_of_experience BETWEEN 6 AND 8 THEN 'SIX_TO_EIGHT'
        ELSE 'NINE_PLUS'
    END,
    specializations = jsonb_build_array(specialization);

ALTER TABLE learners
    ALTER COLUMN experience_band SET NOT NULL,
    ALTER COLUMN specializations SET NOT NULL,
    ADD CONSTRAINT learners_experience_band_check
        CHECK (experience_band IN ('ZERO_TO_ONE', 'TWO_TO_THREE', 'FOUR_TO_FIVE', 'SIX_TO_EIGHT', 'NINE_PLUS')),
    ADD CONSTRAINT learners_specializations_array_check
        CHECK (jsonb_typeof(specializations) = 'array');
