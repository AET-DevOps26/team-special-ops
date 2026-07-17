-- Flyway migration: per-user "liked" series ("My Shows").
-- One row per (user, series). Like watch progress, series_id references the
-- catalog's series by value only (the catalog lives in a separate service/DB),
-- so there is no cross-service foreign key.

CREATE TABLE likes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    series_id  UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, series_id)
);

CREATE INDEX idx_likes_user ON likes(user_id);
