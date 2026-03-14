CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    root_path TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT projects_root_path_key UNIQUE (root_path)
);

CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    workflow_id VARCHAR(255) NOT NULL,
    goal TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_agent_name VARCHAR(128) NOT NULL,
    requested_by_kind VARCHAR(64) NOT NULL,
    requested_by_session_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT tasks_workflow_id_key UNIQUE (workflow_id)
);

CREATE TABLE IF NOT EXISTS agent_sessions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    agent_name VARCHAR(128) NOT NULL,
    provider_session_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT agent_sessions_task_agent_key UNIQUE (task_id, agent_name)
);

CREATE TABLE IF NOT EXISTS task_steps (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    step_number INTEGER NOT NULL,
    agent_name VARCHAR(128) NOT NULL,
    agent_session_id UUID NOT NULL REFERENCES agent_sessions(id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT task_steps_task_step_number_key UNIQUE (task_id, step_number)
);

CREATE TABLE IF NOT EXISTS transcript_records (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    task_step_id UUID NOT NULL REFERENCES task_steps(id),
    agent_session_id UUID NOT NULL REFERENCES agent_sessions(id),
    transcript_kind VARCHAR(64) NOT NULL,
    provider_session_id VARCHAR(255),
    transcript_payload JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT transcript_records_task_step_id_key UNIQUE (task_step_id)
);

CREATE TABLE IF NOT EXISTS task_step_results (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    task_step_id UUID NOT NULL REFERENCES task_steps(id),
    status VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    recommended_next_agent VARCHAR(128) NOT NULL,
    result_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT task_step_results_task_step_id_key UNIQUE (task_step_id)
);

CREATE INDEX IF NOT EXISTS tasks_project_status_idx ON tasks (project_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS task_steps_task_status_idx ON task_steps (task_id, status, step_number DESC);
CREATE INDEX IF NOT EXISTS transcript_records_task_idx ON transcript_records (task_id, created_at DESC);
