CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         TEXT         NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE progress (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    series_id     UUID         NOT NULL,
    episode_index INT          NOT NULL CHECK (episode_index >= 0),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, series_id)
);

CREATE INDEX idx_progress_user ON progress(user_id);
