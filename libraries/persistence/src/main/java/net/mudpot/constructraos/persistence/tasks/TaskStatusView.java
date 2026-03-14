package net.mudpot.constructraos.persistence.tasks;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TaskStatusView(
    UUID taskId,
    String workflowId,
    String projectName,
    String projectRootPath,
    String status,
    String goal,
    String requestedAgentName,
    String requestedByKind,
    String requestedBySessionId,
    Integer latestStepNumber,
    String latestStepStatus,
    String latestStepAgentName,
    String latestStepSummary,
    String recommendedNextAgent,
    String providerSessionId,
    UUID transcriptRecordId,
    Map<String, Object> resultPayload,
    Instant createdAt,
    Instant completedAt
) {
}
