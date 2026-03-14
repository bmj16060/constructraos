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
import java.util.Map;
import java.util.UUID;

@MappedEntity("task_step_results")
public class TaskStepResultEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("task_id")
    private UUID taskId;

    @MappedProperty("task_step_id")
    private UUID taskStepId;

    private String status;

    private String summary;

    @MappedProperty("recommended_next_agent")
    private String recommendedNextAgent;

    @TypeDef(type = DataType.JSON)
    @MappedProperty("result_payload")
    private Map<String, Object> resultPayload;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public String getRecommendedNextAgent() {
        return recommendedNextAgent;
    }

    public void setRecommendedNextAgent(final String recommendedNextAgent) {
        this.recommendedNextAgent = recommendedNextAgent;
    }

    public Map<String, Object> getResultPayload() {
        return resultPayload;
    }

    public void setResultPayload(final Map<String, Object> resultPayload) {
        this.resultPayload = resultPayload;
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
