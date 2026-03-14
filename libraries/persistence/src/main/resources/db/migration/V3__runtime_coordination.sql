CREATE TABLE IF NOT EXISTS runtime_executions (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    task_id UUID NOT NULL REFERENCES tasks(id),
    task_step_id UUID NOT NULL REFERENCES task_steps(id),
    execution_mode VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL,
    awaiting_approval BOOLEAN NOT NULL DEFAULT FALSE,
    owner_instance_id VARCHAR(255) NOT NULL DEFAULT '',
    provider_session_id VARCHAR(255),
    provider_thread_id VARCHAR(255),
    provider_turn_id VARCHAR(255),
    current_request_id VARCHAR(255),
    last_event_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT runtime_executions_workflow_id_key UNIQUE (workflow_id),
    CONSTRAINT runtime_executions_task_step_id_key UNIQUE (task_step_id)
);

CREATE TABLE IF NOT EXISTS runtime_execution_checkpoints (
    id UUID PRIMARY KEY,
    runtime_execution_id UUID NOT NULL REFERENCES runtime_executions(id) ON DELETE CASCADE,
    checkpoint_kind VARCHAR(64) NOT NULL,
    checkpoint_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    checkpointed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT runtime_execution_checkpoints_runtime_execution_id_key UNIQUE (runtime_execution_id)
);

CREATE INDEX IF NOT EXISTS runtime_executions_task_state_idx
    ON runtime_executions (task_id, state, started_at DESC);

CREATE INDEX IF NOT EXISTS runtime_executions_owner_lease_idx
    ON runtime_executions (owner_instance_id, lease_expires_at);
