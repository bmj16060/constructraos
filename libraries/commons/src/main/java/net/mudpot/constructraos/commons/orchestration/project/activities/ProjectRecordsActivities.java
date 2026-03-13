package net.mudpot.constructraos.commons.orchestration.project.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;

import java.util.List;

@ActivityInterface
public interface ProjectRecordsActivities {
    @ActivityMethod
    ProjectTaskRecord loadTask(String projectId, String taskId);

    @ActivityMethod
    ProjectBranchRecord loadBranch(String projectId, String branchName);

    @ActivityMethod
    ProjectEvidenceRecord writeEvidence(ProjectEvidenceWriteRequest request);

    @ActivityMethod
    ProjectExecutionRequestRecord writeExecutionRequest(ProjectExecutionRequestWriteRequest request);

    @ActivityMethod
    List<ProjectExecutionRequestRecord> listExecutionRequests(String projectId, String status);
}
