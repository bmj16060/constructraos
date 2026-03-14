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

    @Query("SELECT * FROM task_steps WHERE task_id = :taskId ORDER BY step_number DESC LIMIT 1")
    Optional<TaskStepEntity> findLatestByTaskId(UUID taskId);
}
