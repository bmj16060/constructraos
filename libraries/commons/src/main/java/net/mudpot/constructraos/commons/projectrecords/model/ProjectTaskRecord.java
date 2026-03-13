package net.mudpot.constructraos.commons.projectrecords.model;

import java.util.List;

public record ProjectTaskRecord(
    String projectId,
    String taskId,
    String title,
    String status,
    String owningSpecialist,
    String parentControlBranch,
    List<String> specialistBranches,
    List<String> linkedAdrs,
    List<String> linkedBugs,
    String path
) {
}
