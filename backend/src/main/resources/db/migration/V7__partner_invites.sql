-- Invitations that let the real person behind a partner record link their
-- own account (consent-based sharing). Only the token hash is stored.

CREATE TABLE partner_invites (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id          UUID        NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    created_by_user_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash          VARCHAR(64) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    accepted_by_user_id UUID        REFERENCES users (id) ON DELETE SET NULL,
    accepted_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_partner_invites_hash UNIQUE (token_hash)
);
CREATE INDEX ix_partner_invites_partner ON partner_invites (partner_id);
