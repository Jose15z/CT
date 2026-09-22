-- Agenda (scheduled dates), intimacy log (encounters) and the optional
-- partner attributes the XP engine reads. XP itself is never stored: it is
-- recomputed deterministically from these rows (single source of truth).

CREATE TABLE date_plans (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id    UUID         NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    title         VARCHAR(120) NOT NULL,
    location      VARCHAR(120),
    notes         TEXT,
    date          DATE         NOT NULL,
    start_time    TIME,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_date_plans_owner_date ON date_plans (owner_user_id, date);
CREATE INDEX ix_date_plans_partner ON date_plans (partner_id);

CREATE TABLE encounters (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id    UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    date          DATE        NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_encounters_owner_date ON encounters (owner_user_id, date DESC);
CREATE INDEX ix_encounters_partner ON encounters (partner_id);

-- Optional, owner-entered partner attributes. The service layer rejects birth
-- dates that would make the partner a minor.
ALTER TABLE partners ADD COLUMN birth_date DATE;
ALTER TABLE partners ADD COLUMN weight_kg NUMERIC(5, 1);
ALTER TABLE partners ADD CONSTRAINT ck_partners_weight
    CHECK (weight_kg IS NULL OR weight_kg BETWEEN 30 AND 300);
