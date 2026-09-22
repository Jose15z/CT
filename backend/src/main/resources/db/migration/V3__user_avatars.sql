-- User profile photos, stored resized (256px square JPEG, ~20KB each).
-- Kept in a separate table so user queries never drag image bytes along.
-- Swappable for object storage (Cloudinary/R2/Supabase) later without
-- touching the API contract.
CREATE TABLE user_avatars (
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    image        BYTEA       NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
