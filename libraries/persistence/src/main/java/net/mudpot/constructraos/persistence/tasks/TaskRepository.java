package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TaskRepository extends CrudRepository<TaskEntity, UUID> {
    Optional<TaskEntity> findByWorkflowId(String workflowId);

    @Query("""
        SELECT *
        FROM tasks
        WHERE project_id = :projectId
        ORDER BY created_at DESC
        LIMIT :limit
        """)
    List<TaskEntity> findRecentByProjectId(UUID projectId, int limit);

    @Query("""
        INSERT INTO tasks (
            id,
            project_id,
            workflow_id,
            goal,
            status,
            requested_agent_name,
            requested_by_kind,
            requested_by_session_id
        ) VALUES (
            :id,
            :projectId,
            :workflowId,
            :goal,
            :status,
            :requestedAgentName,
            :requestedByKind,
            :requestedBySessionId
        )
        ON CONFLICT (workflow_id) DO UPDATE
        SET project_id = EXCLUDED.project_id,
            goal = EXCLUDED.goal,
            status = EXCLUDED.status,
            requested_agent_name = EXCLUDED.requested_agent_name,
            requested_by_kind = EXCLUDED.requested_by_kind,
            requested_by_session_id = EXCLUDED.requested_by_session_id,
            completed_at = NULL,
            updated_at = NOW()
        """)
    void upsertByWorkflowId(
        UUID id,
        UUID projectId,
        String workflowId,
        String goal,
        String status,
        String requestedAgentName,
        String requestedByKind,
        String requestedBySessionId
    );
}
