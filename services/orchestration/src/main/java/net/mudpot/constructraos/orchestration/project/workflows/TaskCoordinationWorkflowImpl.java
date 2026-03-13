package net.mudpot.constructraos.orchestration.project.workflows;

import io.micronaut.context.annotation.Prototype;
import io.temporal.workflow.Workflow;
import jakarta.inject.Named;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskQaRequestSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.orchestration.project.workflows.TaskCoordinationWorkflow;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import net.mudpot.constructraos.orchestration.core.policy.WorkflowPolicyEnforcer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Prototype
public class TaskCoordinationWorkflowImpl implements TaskCoordinationWorkflow {
    private final PolicyEvaluationActivities policyEvaluationActivities;
    private final ProjectRecordsActivities projectRecordsActivities;
    private final List<TaskQaRequestSignal> pendingQaRequests = new ArrayList<>();
    private String projectId = "";
    private String taskId = "";
    private String workflowStatus = "OPEN";
    private String taskStatus = "unknown";
    private String activeBranch = "";
    private String latestEvidenceId = "";
    private String lastEvent = "initialized";
    private int qaRequestCount;
    private boolean closed;

    public TaskCoordinationWorkflowImpl(
        @Named("policyEvaluationActivitiesStub") final PolicyEvaluationActivities policyEvaluationActivities,
        @Named("projectRecordsActivitiesStub") final ProjectRecordsActivities projectRecordsActivities
    ) {
        this.policyEvaluationActivities = policyEvaluationActivities;
        this.projectRecordsActivities = projectRecordsActivities;
    }

    @Override
    public void run(final TaskWorkflowInput input) {
        this.projectId = normalize(input == null ? "" : input.projectId());
        this.taskId = normalize(input == null ? "" : input.taskId());
        this.lastEvent = "workflow_started";
        while (!closed) {
            Workflow.await(() -> closed || !pendingQaRequests.isEmpty());
            while (!pendingQaRequests.isEmpty()) {
                handleQaRequest(pendingQaRequests.removeFirst());
            }
        }
        this.workflowStatus = "CLOSED";
    }

    @Override
    public void requestQa(final TaskQaRequestSignal request) {
        pendingQaRequests.add(normalizedSignal(request));
        lastEvent = "qa_signal_received";
    }

    @Override
    public void close(final String reason) {
        closed = true;
        lastEvent = normalize(reason).isBlank() ? "workflow_closed" : normalize(reason);
    }

    @Override
    public TaskWorkflowState currentState() {
        return new TaskWorkflowState(projectId, taskId, workflowStatus, taskStatus, activeBranch, latestEvidenceId, lastEvent, qaRequestCount);
    }

    private void handleQaRequest(final TaskQaRequestSignal request) {
        WorkflowPolicyEnforcer.enforce(
            policyEvaluationActivities,
            "workflow.task.request_qa",
            Map.of(
                "resource_name", "task_workflow",
                "actor", Map.of(
                    "kind", normalize(request.requestedByKind()),
                    "session_id", normalize(request.sessionId())
                ),
                "request", Map.of(
                    "project_id", projectId,
                    "task_id", taskId,
                    "branch_name", normalize(request.branchName())
                )
            )
        );
        final ProjectTaskRecord taskRecord = projectRecordsActivities.loadTask(projectId, taskId);
        final String branchName = resolveBranchName(taskRecord, request.branchName());
        final ProjectBranchRecord branchRecord = resolveBranchRecord(branchName);
        final ProjectEvidenceRecord evidenceRecord = projectRecordsActivities.writeQaEvidence(
            new ProjectEvidenceWriteRequest(
                projectId,
                taskId,
                branchName,
                branchRecord.environment(),
                "QA",
                "requested",
                List.of(
                    "QA request accepted by the long-running task workflow.",
                    "Branch and task records were loaded from the repo-backed project contract.",
                    "Awaiting SRE environment preparation and smoke validation."
                ),
                normalize(request.note()).isBlank() ? "No additional notes." : normalize(request.note()),
                normalize(request.requestedByKind()),
                normalize(request.sessionId()),
                Workflow.getInfo().getWorkflowId()
            )
        );
        this.taskStatus = taskRecord.status();
        this.activeBranch = branchName;
        this.latestEvidenceId = evidenceRecord.id();
        this.qaRequestCount += 1;
        this.lastEvent = "qa_requested";
    }

    private ProjectBranchRecord resolveBranchRecord(final String branchName) {
        try {
            return projectRecordsActivities.loadBranch(projectId, branchName);
        } catch (RuntimeException ignored) {
            return new ProjectBranchRecord(projectId, branchName, "unresolved", "task branch from workflow request", "environment unresolved", "unknown");
        }
    }

    private static String resolveBranchName(final ProjectTaskRecord taskRecord, final String requestedBranchName) {
        final String normalizedRequestedBranch = normalize(requestedBranchName);
        if (!normalizedRequestedBranch.isBlank()) {
            return normalizedRequestedBranch;
        }
        return normalize(taskRecord.parentControlBranch());
    }

    private static TaskQaRequestSignal normalizedSignal(final TaskQaRequestSignal request) {
        if (request == null) {
            return new TaskQaRequestSignal("", "", "", "");
        }
        return new TaskQaRequestSignal(
            normalize(request.branchName()),
            normalize(request.requestedByKind()),
            normalize(request.sessionId()),
            normalize(request.note())
        );
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
