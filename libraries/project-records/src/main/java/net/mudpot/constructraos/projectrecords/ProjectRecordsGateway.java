package net.mudpot.constructraos.projectrecords;

import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionClaimRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;

import java.util.List;

public interface ProjectRecordsGateway {
    ProjectTaskRecord loadTask(String projectId, String taskId);

    ProjectBranchRecord loadBranch(String projectId, String branchName);

    ProjectEnvironmentRecord loadEnvironment(String projectId, String environmentId);

    ProjectEvidenceRecord writeEvidence(ProjectEvidenceWriteRequest request);

    ProjectEnvironmentRecord writeEnvironment(ProjectEnvironmentWriteRequest request);

    ProjectExecutionRequestRecord writeExecutionRequest(ProjectExecutionRequestWriteRequest request);

    List<ProjectEnvironmentRecord> listEnvironments(String projectId, String status);

    List<ProjectExecutionRequestRecord> listExecutionRequests(String projectId, String status);

    ProjectExecutionRequestRecord claimExecutionRequest(ProjectExecutionClaimRequest request);
}
