package net.mudpot.constructraos.persistence.history;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PromptRunRepository extends CrudRepository<PromptRunEntity, UUID> {
    @Query("SELECT * FROM prompt_runs ORDER BY created_at DESC LIMIT :limit")
    List<PromptRunEntity> findRecent(int limit);
}
