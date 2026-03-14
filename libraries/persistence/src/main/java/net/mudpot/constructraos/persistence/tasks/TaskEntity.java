package net.mudpot.constructraos.persistence.tasks;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("tasks")
public class TaskEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("project_id")
    private UUID projectId;

    @MappedProperty("workflow_id")
    private String workflowId;

    private String goal;

    private String status;

    @MappedProperty("requested_agent_name")
    private String requestedAgentName;

    @MappedProperty("requested_by_kind")
    private String requestedByKind;

    @MappedProperty("requested_by_session_id")
    private String requestedBySessionId;

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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(final UUID projectId) {
        this.projectId = projectId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(final String goal) {
        this.goal = goal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getRequestedAgentName() {
        return requestedAgentName;
    }

    public void setRequestedAgentName(final String requestedAgentName) {
        this.requestedAgentName = requestedAgentName;
    }

    public String getRequestedByKind() {
        return requestedByKind;
    }

    public void setRequestedByKind(final String requestedByKind) {
        this.requestedByKind = requestedByKind;
    }

    public String getRequestedBySessionId() {
        return requestedBySessionId;
    }

    public void setRequestedBySessionId(final String requestedBySessionId) {
        this.requestedBySessionId = requestedBySessionId;
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
