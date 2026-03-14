package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface AgentSessionRepository extends CrudRepository<AgentSessionEntity, UUID> {
    Optional<AgentSessionEntity> findByTaskIdAndAgentName(UUID taskId, String agentName);

    @Query("""
        INSERT INTO agent_sessions (
            id,
            task_id,
            agent_name,
            provider_session_id
        ) VALUES (
            :id,
            :taskId,
            :agentName,
            :providerSessionId
        )
        ON CONFLICT (task_id, agent_name) DO UPDATE
        SET provider_session_id = CASE
            WHEN EXCLUDED.provider_session_id = '' THEN agent_sessions.provider_session_id
            ELSE EXCLUDED.provider_session_id
        END,
            updated_at = NOW()
        """)
    void upsertByTaskAndAgentName(UUID id, UUID taskId, String agentName, String providerSessionId);
}
