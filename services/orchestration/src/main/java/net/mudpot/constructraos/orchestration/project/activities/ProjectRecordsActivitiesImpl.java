package net.mudpot.constructraos.orchestration.project.activities;

import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;

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
    public ProjectEvidenceRecord writeEvidence(final ProjectEvidenceWriteRequest request) {
        return projectRecordsGateway.writeEvidence(request);
    }

    @Override
    public ProjectExecutionRequestRecord writeExecutionRequest(final ProjectExecutionRequestWriteRequest request) {
        return projectRecordsGateway.writeExecutionRequest(request);
    }
}
