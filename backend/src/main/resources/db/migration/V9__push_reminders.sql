-- Web Push subscriptions (one row per browser/device) and a send log that
-- guarantees each reminder kind goes out at most once per user per day.

CREATE TABLE push_subscriptions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   TEXT         NOT NULL,
    p256dh     VARCHAR(255) NOT NULL,
    auth       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_push_subscriptions_endpoint UNIQUE (endpoint)
);
CREATE INDEX ix_push_subscriptions_user ON push_subscriptions (user_id);

CREATE TABLE reminder_sends (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind    VARCHAR(80) NOT NULL,
    day     DATE        NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, kind, day)
);

-- Reminders fire in the user's local time; default keeps existing users sane.
ALTER TABLE users ADD COLUMN timezone VARCHAR(60) NOT NULL DEFAULT 'America/Bogota';
ALTER TABLE users ADD COLUMN reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE;
