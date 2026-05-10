ALTER TABLE custom_validation_rules
    ADD COLUMN IF NOT EXISTS ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS generation_prompt TEXT,
    ADD COLUMN IF NOT EXISTS generation_model VARCHAR(64);
