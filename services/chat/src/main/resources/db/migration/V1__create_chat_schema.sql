-- Chat service: persisted Q&A history.

CREATE TABLE chat_question (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL,
    series_id       UUID         NOT NULL,
    question_text   TEXT         NOT NULL,
    progress_at_ask INT          NOT NULL CHECK (progress_at_ask >= 0),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_answer (
    id                      UUID         PRIMARY KEY,
    question_id             UUID         NOT NULL REFERENCES chat_question(id) ON DELETE CASCADE,
    answer_text             TEXT         NOT NULL,
    cited_episode_indices   JSONB        NOT NULL DEFAULT '[]',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_question_user_series ON chat_question(user_id, series_id);
