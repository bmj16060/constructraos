package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectBranchRecord(
    String projectId,
    String branchName,
    String role,
    String scope,
    String environment,
    String status
) {
}
