-- Catalog service: shows and episodes.
-- All catalog-owned tables live in the default 'public' schema for v0.
-- The episode.episode_index column is the global 1..N index used for the
-- spoiler-safe filter `episode_index <= progress` in the chat service.

CREATE TABLE show (
    id              UUID         PRIMARY KEY,
    title           TEXT         NOT NULL UNIQUE,
    seasons_count   INT          NOT NULL,
    episodes_count  INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE episode (
    id              UUID         PRIMARY KEY,
    show_id         UUID         NOT NULL REFERENCES show(id) ON DELETE CASCADE,
    season          INT          NOT NULL,
    episode_number  INT          NOT NULL,
    episode_index   INT          NOT NULL,
    title           TEXT         NOT NULL,
    summary         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    UNIQUE (show_id, season, episode_number),
    UNIQUE (show_id, episode_index)
);

CREATE INDEX idx_episode_show_index ON episode(show_id, episode_index);
