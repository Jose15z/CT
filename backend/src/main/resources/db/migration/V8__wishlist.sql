-- Gift ideas the owner jots down per partner ("things she mentioned").
-- Private to the owner; surfaced by the advice engine before birthdays and
-- anniversaries.

CREATE TABLE wishlist_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id    UUID         NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    title         VARCHAR(120) NOT NULL,
    note          TEXT,
    url           VARCHAR(500),
    done          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_wishlist_partner ON wishlist_items (partner_id, done);
