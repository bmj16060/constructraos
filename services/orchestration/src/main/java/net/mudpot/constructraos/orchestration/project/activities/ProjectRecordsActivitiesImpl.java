package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;

import java.util.List;

@Singleton
public class ProjectRecordsActivitiesImpl implements ProjectRecordsActivities {
    private final ProjectRecordsGateway projectRecordsGateway;

    public ProjectRecordsActivitiesImpl(final ProjectRecordsGateway projectRecordsGateway) {
        this.projectRecordsGateway = projectRecordsGateway;
    }

    @Override
    public ProjectTaskRecord loadTask(final String projectId, final String taskId) {
        return projectRecordsGateway.loadTask(projectId, taskId);
    }

    @Override
    public ProjectBranchRecord loadBranch(final String projectId, final String branchName) {
        return projectRecordsGateway.loadBranch(projectId, branchName);
    }

    @Override
    public ProjectEnvironmentRecord loadEnvironment(final String projectId, final String environmentId) {
        return projectRecordsGateway.loadEnvironment(projectId, environmentId);
    }

    @Override
    public ProjectEvidenceRecord writeEvidence(final ProjectEvidenceWriteRequest request) {
        return projectRecordsGateway.writeEvidence(request);
    }

    @Override
    public ProjectEnvironmentRecord writeEnvironment(final ProjectEnvironmentWriteRequest request) {
        return projectRecordsGateway.writeEnvironment(request);
    }

    @Override
    public ProjectExecutionRequestRecord writeExecutionRequest(final ProjectExecutionRequestWriteRequest request) {
        return projectRecordsGateway.writeExecutionRequest(request);
    }

    @Override
    public List<ProjectEnvironmentRecord> listEnvironments(final String projectId, final String status) {
        return projectRecordsGateway.listEnvironments(projectId, status);
    }

    @Override
    public List<ProjectExecutionRequestRecord> listExecutionRequests(final String projectId, final String status) {
        return projectRecordsGateway.listExecutionRequests(projectId, status);
    }
}
