CREATE TABLE series (
    id              UUID         PRIMARY KEY,
    title           TEXT         NOT NULL UNIQUE,
    seasons_count   INT          NOT NULL,
    episodes_count  INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE episode (
    id              UUID         PRIMARY KEY,
    series_id       UUID         NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    season          INT          NOT NULL,
    episode_number  INT          NOT NULL,
    episode_index   INT          NOT NULL,
    title           TEXT         NOT NULL,
    summary         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (series_id, season, episode_number),
    UNIQUE (series_id, episode_index)
);

CREATE INDEX idx_episode_series_index ON episode(series_id, episode_index);
