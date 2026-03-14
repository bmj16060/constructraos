package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ProjectRepository extends CrudRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByRootPath(String rootPath);

    @Query("""
        INSERT INTO projects (
            id,
            name,
            root_path
        ) VALUES (
            :id,
            :name,
            :rootPath
        )
        ON CONFLICT (root_path) DO UPDATE
        SET name = EXCLUDED.name,
            updated_at = NOW()
        """)
    void upsertByRootPath(UUID id, String name, String rootPath);
}
