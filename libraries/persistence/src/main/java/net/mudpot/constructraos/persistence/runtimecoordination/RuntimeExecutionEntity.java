package net.mudpot.constructraos.persistence.runtimecoordination;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("runtime_executions")
public class RuntimeExecutionEntity {
    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("workflow_id")
    private String workflowId;

    @MappedProperty("task_id")
    private UUID taskId;

    @MappedProperty("task_step_id")
    private UUID taskStepId;

    @MappedProperty("execution_mode")
    private String executionMode;

    private String state;

    @MappedProperty("awaiting_approval")
    private boolean awaitingApproval;

    @MappedProperty("owner_instance_id")
    private String ownerInstanceId;

    @MappedProperty("provider_session_id")
    private String providerSessionId;

    @MappedProperty("provider_thread_id")
    private String providerThreadId;

    @MappedProperty("provider_turn_id")
    private String providerTurnId;

    @MappedProperty("current_request_id")
    private String currentRequestId;

    @MappedProperty("last_event_at")
    private Instant lastEventAt;

    @MappedProperty("last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @MappedProperty("lease_expires_at")
    private Instant leaseExpiresAt;

    @MappedProperty("started_at")
    private Instant startedAt;

    @MappedProperty("completed_at")
    private Instant completedAt;

    @MappedProperty("failure_reason")
    private String failureReason;

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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
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

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(final String executionMode) {
        this.executionMode = executionMode;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public boolean isAwaitingApproval() {
        return awaitingApproval;
    }

    public void setAwaitingApproval(final boolean awaitingApproval) {
        this.awaitingApproval = awaitingApproval;
    }

    public String getOwnerInstanceId() {
        return ownerInstanceId;
    }

    public void setOwnerInstanceId(final String ownerInstanceId) {
        this.ownerInstanceId = ownerInstanceId;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public void setProviderSessionId(final String providerSessionId) {
        this.providerSessionId = providerSessionId;
    }

    public String getProviderThreadId() {
        return providerThreadId;
    }

    public void setProviderThreadId(final String providerThreadId) {
        this.providerThreadId = providerThreadId;
    }

    public String getProviderTurnId() {
        return providerTurnId;
    }

    public void setProviderTurnId(final String providerTurnId) {
        this.providerTurnId = providerTurnId;
    }

    public String getCurrentRequestId() {
        return currentRequestId;
    }

    public void setCurrentRequestId(final String currentRequestId) {
        this.currentRequestId = currentRequestId;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(final Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(final Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(final Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
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
