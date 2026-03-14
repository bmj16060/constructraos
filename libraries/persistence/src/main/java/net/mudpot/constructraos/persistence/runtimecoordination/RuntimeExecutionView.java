package net.mudpot.constructraos.persistence.runtimecoordination;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuntimeExecutionView(
    UUID id,
    String workflowId,
    UUID taskId,
    UUID taskStepId,
    String executionMode,
    String state,
    boolean awaitingApproval,
    String ownerInstanceId,
    String providerSessionId,
    String providerThreadId,
    String providerTurnId,
    String currentRequestId,
    Instant lastEventAt,
    Instant lastHeartbeatAt,
    Instant leaseExpiresAt,
    Instant startedAt,
    Instant completedAt,
    String failureReason,
    String checkpointKind,
    Map<String, Object> checkpointPayload,
    Instant checkpointedAt
) {
}
