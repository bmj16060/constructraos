package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TaskStepResultRepository extends CrudRepository<TaskStepResultEntity, UUID> {
    Optional<TaskStepResultEntity> findByTaskStepId(UUID taskStepId);
}
