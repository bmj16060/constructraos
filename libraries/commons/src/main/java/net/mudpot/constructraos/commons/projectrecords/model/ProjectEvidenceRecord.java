package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectEvidenceRecord(
    String id,
    String path,
    String projectId,
    String taskId,
    String branchName,
    String environment,
    String status,
    String validatingSpecialist,
    String createdAt
) {
}
