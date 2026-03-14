package net.mudpot.constructraos.persistence.runtimecoordination;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface RuntimeExecutionCheckpointRepository extends CrudRepository<RuntimeExecutionCheckpointEntity, UUID> {
    Optional<RuntimeExecutionCheckpointEntity> findByRuntimeExecutionId(UUID runtimeExecutionId);
}
