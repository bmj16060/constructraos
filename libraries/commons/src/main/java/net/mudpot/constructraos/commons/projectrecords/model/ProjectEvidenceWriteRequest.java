package net.mudpot.constructraos.commons.projectrecords.model;

import java.util.List;

public record ProjectEvidenceWriteRequest(
    String projectId,
    String taskId,
    String branchName,
    String environment,
    String validatingSpecialist,
    String status,
    List<String> executedChecks,
    String note,
    String actorKind,
    String sessionId,
    String workflowId
) {
}
