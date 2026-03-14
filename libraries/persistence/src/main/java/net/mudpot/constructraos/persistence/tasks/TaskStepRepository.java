package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TaskStepRepository extends CrudRepository<TaskStepEntity, UUID> {
    Optional<TaskStepEntity> findByTaskIdAndStepNumber(UUID taskId, int stepNumber);

    @Query("""
        INSERT INTO task_steps (
            id,
            task_id,
            step_number,
            agent_name,
            agent_session_id,
            status
        ) VALUES (
            :id,
            :taskId,
            :stepNumber,
            :agentName,
            :agentSessionId,
            :status
        )
        ON CONFLICT (task_id, step_number) DO UPDATE
        SET agent_name = EXCLUDED.agent_name,
            agent_session_id = EXCLUDED.agent_session_id,
            status = EXCLUDED.status,
            completed_at = NULL,
            updated_at = NOW()
        """)
    void upsertByTaskAndStepNumber(
        UUID id,
        UUID taskId,
        int stepNumber,
        String agentName,
        UUID agentSessionId,
        String status
    );

    @Query("SELECT * FROM task_steps WHERE task_id = :taskId ORDER BY step_number DESC LIMIT 1")
    Optional<TaskStepEntity> findLatestByTaskId(UUID taskId);
}
