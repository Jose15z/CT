-- Plans and the mirror of the Stripe subscription that grants them. The
-- plan column is the single source of truth for entitlements; webhooks keep
-- it in sync and an operator can flip it by hand.

ALTER TABLE users ADD COLUMN plan VARCHAR(10) NOT NULL DEFAULT 'FREE';
ALTER TABLE users ADD CONSTRAINT ck_users_plan CHECK (plan IN ('FREE', 'PRO'));

CREATE TABLE subscriptions (
    user_id                  UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    provider                 VARCHAR(20)  NOT NULL DEFAULT 'STRIPE',
    provider_customer_id     VARCHAR(120),
    provider_subscription_id VARCHAR(120),
    status                   VARCHAR(30)  NOT NULL,
    current_period_end       TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscriptions_provider_sub UNIQUE (provider_subscription_id)
);
