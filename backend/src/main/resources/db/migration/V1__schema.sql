-- CulitosTracker initial schema.
-- Conventions: UUID PKs, timestamptz audit columns, civil dates as DATE.

CREATE TABLE users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username               VARCHAR(30)  NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,
    display_name           VARCHAR(60)  NOT NULL,
    preferred_language     VARCHAR(5)   NOT NULL DEFAULT 'es',
    avatar_emoji           VARCHAR(16),
    relationship_situation VARCHAR(20)  NOT NULL DEFAULT 'SINGLE',
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_situation CHECK (relationship_situation IN
        ('SINGLE', 'IN_RELATIONSHIP', 'MARRIED', 'POLYAMOROUS', 'OTHER'))
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE partners (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    linked_user_id  UUID        REFERENCES users (id) ON DELETE SET NULL,
    name            VARCHAR(60) NOT NULL,
    normalized_name VARCHAR(60) NOT NULL,
    nickname        VARCHAR(60),
    notes           TEXT,
    avatar_emoji    VARCHAR(16),
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_partners_owner ON partners (owner_user_id);
CREATE INDEX ix_partners_linked ON partners (linked_user_id);
-- Same person cannot be registered twice while active. Soft-deleted rows are
-- kept for history and for the leaderboard's unique-partner metric.
CREATE UNIQUE INDEX uq_partners_owner_name_active
    ON partners (owner_user_id, normalized_name)
    WHERE deleted_at IS NULL;

CREATE TABLE relationships (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id              UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    type                    VARCHAR(30) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    dating_start_date       DATE,
    relationship_start_date DATE,
    engagement_date         DATE,
    marriage_date           DATE,
    relationship_end_date   DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_relationships_partner UNIQUE (partner_id),
    CONSTRAINT ck_relationships_type CHECK (type IN
        ('CASUAL', 'DATING', 'SERIOUS_RELATIONSHIP', 'MONOGAMOUS', 'POLYAMOROUS',
         'ENGAGED', 'MARRIED', 'FRIENDS_WITH_BENEFITS', 'OTHER')),
    CONSTRAINT ck_relationships_status CHECK (status IN
        ('ACTIVE', 'PAUSED', 'INACTIVE', 'ENDED'))
);

CREATE TABLE relationship_milestones (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id  UUID         NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(120) NOT NULL,
    description TEXT,
    date        DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_milestones_type CHECK (type IN
        ('FIRST_DATE', 'STARTED_DATING', 'RELATIONSHIP_STARTED', 'ENGAGEMENT',
         'MARRIAGE', 'MOVED_IN_TOGETHER', 'TRIP', 'CUSTOM'))
);
CREATE INDEX ix_milestones_partner_date ON relationship_milestones (partner_id, date);

CREATE TABLE cycle_profiles (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id             UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    average_cycle_length   INT         NOT NULL DEFAULT 28,
    average_period_length  INT         NOT NULL DEFAULT 5,
    last_period_start_date DATE,
    tracking_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cycle_profiles_partner UNIQUE (partner_id),
    CONSTRAINT ck_cycle_length CHECK (average_cycle_length BETWEEN 15 AND 60),
    CONSTRAINT ck_period_length CHECK (average_period_length BETWEEN 1 AND 12)
);

CREATE TABLE period_records (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_profile_id UUID        NOT NULL REFERENCES cycle_profiles (id) ON DELETE CASCADE,
    start_date       DATE        NOT NULL,
    end_date         DATE,
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_period_records_start UNIQUE (cycle_profile_id, start_date),
    CONSTRAINT ck_period_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX ix_period_records_profile_start ON period_records (cycle_profile_id, start_date DESC);

CREATE TABLE relationship_check_ins (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    relationship_id           UUID        NOT NULL REFERENCES relationships (id) ON DELETE CASCADE,
    author_user_id            UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    mood                      VARCHAR(20) NOT NULL,
    energy_level              INT         NOT NULL,
    stress_level              INT         NOT NULL,
    affection_level           INT,
    relationship_satisfaction INT,
    note                      TEXT,
    check_in_date             DATE        NOT NULL DEFAULT CURRENT_DATE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_check_ins_daily UNIQUE (relationship_id, author_user_id, check_in_date),
    CONSTRAINT ck_check_ins_mood CHECK (mood IN
        ('VERY_HAPPY', 'HAPPY', 'CALM', 'NEUTRAL', 'TIRED', 'STRESSED', 'SAD',
         'ANGRY', 'ANXIOUS', 'OVERWHELMED', 'AFFECTIONATE', 'CUSTOM')),
    CONSTRAINT ck_check_ins_energy CHECK (energy_level BETWEEN 1 AND 5),
    CONSTRAINT ck_check_ins_stress CHECK (stress_level BETWEEN 1 AND 5),
    CONSTRAINT ck_check_ins_affection CHECK (affection_level IS NULL OR affection_level BETWEEN 1 AND 5),
    CONSTRAINT ck_check_ins_satisfaction CHECK (relationship_satisfaction IS NULL OR relationship_satisfaction BETWEEN 1 AND 5)
);
CREATE INDEX ix_check_ins_relationship_date ON relationship_check_ins (relationship_id, check_in_date DESC);

CREATE TABLE partner_observations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    observer_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id       UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    observation_type VARCHAR(30) NOT NULL,
    note             TEXT,
    observation_date DATE        NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_observations_type CHECK (observation_type IN
        ('VERY_HAPPY', 'HAPPY', 'NEUTRAL', 'TIRED', 'STRESSED', 'SAD', 'UPSET',
         'DISTANT', 'AFFECTIONATE', 'NEEDS_SPACE', 'NOT_SURE', 'OTHER'))
);
CREATE INDEX ix_observations_partner_created ON partner_observations (partner_id, created_at DESC);

CREATE TABLE partner_access (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id         UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    granted_by_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    granted_to_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scope              VARCHAR(30) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_partner_access UNIQUE (partner_id, granted_to_user_id, scope),
    CONSTRAINT ck_partner_access_scope CHECK (scope IN ('CYCLE', 'CHECK_INS')),
    CONSTRAINT ck_partner_access_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);
CREATE INDEX ix_partner_access_grantee ON partner_access (granted_to_user_id);

CREATE TABLE daily_tips (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key        VARCHAR(120) NOT NULL,
    category           VARCHAR(30)  NOT NULL,
    cycle_phase        VARCHAR(20),
    relationship_stage VARCHAR(20),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_tips_key UNIQUE (message_key),
    CONSTRAINT ck_daily_tips_phase CHECK (cycle_phase IS NULL OR cycle_phase IN
        ('MENSTRUATION', 'FOLLICULAR', 'OVULATION', 'LUTEAL')),
    CONSTRAINT ck_daily_tips_stage CHECK (relationship_stage IS NULL OR relationship_stage IN
        ('NEW', 'DEVELOPING', 'ESTABLISHED', 'LONG_TERM', 'VERY_LONG_TERM'))
);

CREATE TABLE leaderboard_profiles (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    enabled      BOOLEAN     NOT NULL DEFAULT FALSE,
    public_alias VARCHAR(30),
    show_avatar  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
