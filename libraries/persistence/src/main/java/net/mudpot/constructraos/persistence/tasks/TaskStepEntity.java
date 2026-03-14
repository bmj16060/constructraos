package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("task_steps")
public class TaskStepEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("task_id")
    private UUID taskId;

    @MappedProperty("step_number")
    private int stepNumber;

    @MappedProperty("agent_name")
    private String agentName;

    @MappedProperty("agent_session_id")
    private UUID agentSessionId;

    private String status;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    @MappedProperty("completed_at")
    private Instant completedAt;

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

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(final int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(final String agentName) {
        this.agentName = agentName;
    }

    public UUID getAgentSessionId() {
        return agentSessionId;
    }

    public void setAgentSessionId(final UUID agentSessionId) {
        this.agentSessionId = agentSessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }
}
