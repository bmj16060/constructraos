package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectEnvironmentWriteRequest(
    String projectId,
    String environmentId,
    String taskId,
    String branchName,
    String environmentName,
    String namespace,
    String ownershipScope,
    String status,
    boolean protectedEnvironment,
    String lastActiveAt,
    String retireAfter,
    String note
) {
}
