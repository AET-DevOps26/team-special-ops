-- Flyway migration: per-series watch progress.
-- One high-water-mark row per (user, series): episode_index is the highest
-- catalog episode_index the user has watched (0 = nothing watched).
-- series_id references the catalog's series by value; it is NOT a foreign key
-- because the catalog lives in a separate service/database.

CREATE TABLE progress (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    series_id     UUID         NOT NULL,
    episode_index INT          NOT NULL CHECK (episode_index >= 0),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, series_id)
);

CREATE INDEX idx_progress_user ON progress(user_id);
