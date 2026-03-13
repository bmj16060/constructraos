package net.mudpot.constructraos.projectrecords;

import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;

public interface ProjectRecordsGateway {
    ProjectTaskRecord loadTask(String projectId, String taskId);

    ProjectBranchRecord loadBranch(String projectId, String branchName);

    ProjectEvidenceRecord writeEvidence(ProjectEvidenceWriteRequest request);
}
