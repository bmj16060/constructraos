CREATE TABLE IF NOT EXISTS prompt_runs (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    prompt_template VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    use_case TEXT NOT NULL,
    prompt_text TEXT NOT NULL,
    response_text TEXT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS prompt_runs_created_at_idx ON prompt_runs (created_at DESC);
