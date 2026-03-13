package net.mudpot.constructraos.commons.orchestration.project.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentWriteRequest;
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
    ProjectEnvironmentRecord loadEnvironment(String projectId, String environmentId);

    @ActivityMethod
    ProjectEvidenceRecord writeEvidence(ProjectEvidenceWriteRequest request);

    @ActivityMethod
    ProjectEnvironmentRecord writeEnvironment(ProjectEnvironmentWriteRequest request);

    @ActivityMethod
    ProjectExecutionRequestRecord writeExecutionRequest(ProjectExecutionRequestWriteRequest request);

    @ActivityMethod
    List<ProjectEnvironmentRecord> listEnvironments(String projectId, String status);

    @ActivityMethod
    List<ProjectExecutionRequestRecord> listExecutionRequests(String projectId, String status);
}
