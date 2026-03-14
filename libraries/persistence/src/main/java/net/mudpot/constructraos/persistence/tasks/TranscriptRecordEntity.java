package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@MappedEntity("transcript_records")
public class TranscriptRecordEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("task_id")
    private UUID taskId;

    @MappedProperty("task_step_id")
    private UUID taskStepId;

    @MappedProperty("agent_session_id")
    private UUID agentSessionId;

    @MappedProperty("transcript_kind")
    private String transcriptKind;

    @MappedProperty("provider_session_id")
    private String providerSessionId;

    @TypeDef(type = DataType.JSON)
    @MappedProperty("transcript_payload")
    private List<String> transcriptPayload;

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

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(final UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getTaskStepId() {
        return taskStepId;
    }

    public void setTaskStepId(final UUID taskStepId) {
        this.taskStepId = taskStepId;
    }

    public UUID getAgentSessionId() {
        return agentSessionId;
    }

    public void setAgentSessionId(final UUID agentSessionId) {
        this.agentSessionId = agentSessionId;
    }

    public String getTranscriptKind() {
        return transcriptKind;
    }

    public void setTranscriptKind(final String transcriptKind) {
        this.transcriptKind = transcriptKind;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public void setProviderSessionId(final String providerSessionId) {
        this.providerSessionId = providerSessionId;
    }

    public List<String> getTranscriptPayload() {
        return transcriptPayload;
    }

    public void setTranscriptPayload(final List<String> transcriptPayload) {
        this.transcriptPayload = transcriptPayload;
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
