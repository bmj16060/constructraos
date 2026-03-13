package net.mudpot.constructraos.commons.orchestration.project.activities;

import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;

public interface ProjectRecordsActivities {
    ProjectTaskRecord loadTask(String projectId, String taskId);

    ProjectBranchRecord loadBranch(String projectId, String branchName);

    ProjectEvidenceRecord writeQaEvidence(ProjectEvidenceWriteRequest request);
}
