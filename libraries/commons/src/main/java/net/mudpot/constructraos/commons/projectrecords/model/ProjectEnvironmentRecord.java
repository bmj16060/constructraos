package net.mudpot.constructraos.commons.projectrecords.model;

public record ProjectEnvironmentRecord(
    String id,
    String path,
    String projectId,
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
