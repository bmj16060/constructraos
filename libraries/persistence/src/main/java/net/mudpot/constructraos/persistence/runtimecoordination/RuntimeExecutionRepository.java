package net.mudpot.constructraos.persistence.runtimecoordination;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface RuntimeExecutionRepository extends CrudRepository<RuntimeExecutionEntity, UUID> {
    Optional<RuntimeExecutionEntity> findByWorkflowId(String workflowId);

    @Query("""
        INSERT INTO runtime_executions (
            id,
            workflow_id,
            task_id,
            task_step_id,
            execution_mode,
            state,
            awaiting_approval,
            owner_instance_id,
            started_at,
            last_event_at
        ) VALUES (
            :id,
            :workflowId,
            :taskId,
            :taskStepId,
            :executionMode,
            :state,
            FALSE,
            '',
            NOW(),
            NOW()
        )
        ON CONFLICT (workflow_id) DO UPDATE
        SET task_id = EXCLUDED.task_id,
            task_step_id = EXCLUDED.task_step_id,
            execution_mode = EXCLUDED.execution_mode,
            state = EXCLUDED.state,
            awaiting_approval = FALSE,
            owner_instance_id = '',
            provider_session_id = NULL,
            provider_thread_id = NULL,
            provider_turn_id = NULL,
            current_request_id = NULL,
            last_event_at = NOW(),
            last_heartbeat_at = NULL,
            lease_expires_at = NULL,
            completed_at = NULL,
            failure_reason = NULL,
            updated_at = NOW()
        """)
    void upsertStarted(UUID id, String workflowId, UUID taskId, UUID taskStepId, String executionMode, String state);
}
