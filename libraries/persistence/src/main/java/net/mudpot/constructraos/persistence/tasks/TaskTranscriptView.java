package net.mudpot.constructraos.persistence.tasks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskTranscriptView(
    UUID taskId,
    String workflowId,
    UUID transcriptRecordId,
    Integer latestStepNumber,
    String latestStepStatus,
    String latestStepAgentName,
    String transcriptKind,
    String providerSessionId,
    List<String> transcriptPayload,
    Instant createdAt,
    Instant updatedAt
) {
}
