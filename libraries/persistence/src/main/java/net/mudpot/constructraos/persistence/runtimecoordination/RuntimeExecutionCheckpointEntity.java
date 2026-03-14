package net.mudpot.constructraos.persistence.runtimecoordination;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@MappedEntity("runtime_execution_checkpoints")
public class RuntimeExecutionCheckpointEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("runtime_execution_id")
    private UUID runtimeExecutionId;

    @MappedProperty("checkpoint_kind")
    private String checkpointKind;

    @TypeDef(type = DataType.JSON)
    @MappedProperty("checkpoint_payload")
    private Map<String, Object> checkpointPayload;

    @MappedProperty("checkpointed_at")
    private Instant checkpointedAt;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getRuntimeExecutionId() {
        return runtimeExecutionId;
    }

    public void setRuntimeExecutionId(final UUID runtimeExecutionId) {
        this.runtimeExecutionId = runtimeExecutionId;
    }

    public String getCheckpointKind() {
        return checkpointKind;
    }

    public void setCheckpointKind(final String checkpointKind) {
        this.checkpointKind = checkpointKind;
    }

    public Map<String, Object> getCheckpointPayload() {
        return checkpointPayload;
    }

    public void setCheckpointPayload(final Map<String, Object> checkpointPayload) {
        this.checkpointPayload = checkpointPayload;
    }

    public Instant getCheckpointedAt() {
        return checkpointedAt;
    }

    public void setCheckpointedAt(final Instant checkpointedAt) {
        this.checkpointedAt = checkpointedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
