package net.mudpot.constructraos.orchestration.project.workflows;

import io.micronaut.context.annotation.Prototype;
import io.temporal.workflow.Workflow;
import jakarta.inject.Named;
import net.mudpot.constructraos.commons.orchestration.project.activities.CodexActivities;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionAcceptedSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskQaRequestSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskSreEnvironmentOutcomeSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.orchestration.project.workflows.TaskCoordinationWorkflow;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import net.mudpot.constructraos.orchestration.core.policy.WorkflowPolicyEnforcer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Prototype
public class TaskCoordinationWorkflowImpl implements TaskCoordinationWorkflow {
    private final PolicyEvaluationActivities policyEvaluationActivities;
    private final CodexActivities codexActivities;
    private final ProjectRecordsActivities projectRecordsActivities;
    private final List<TaskQaRequestSignal> pendingQaRequests = new ArrayList<>();
    private final List<CodexExecutionAcceptedSignal> pendingCodexAcceptedSignals = new ArrayList<>();
    private final List<TaskSreEnvironmentOutcomeSignal> pendingSreEnvironmentOutcomes = new ArrayList<>();
    private String projectId = "";
    private String taskId = "";
    private String workflowStatus = "OPEN";
    private String taskStatus = "unknown";
    private String waitingOn = "NONE";
    private String activeBranch = "";
    private String environmentStatus = "unknown";
    private String environmentName = "";
    private String activeExecutionRequestId = "";
    private String codexThreadId = "";
    private String latestEvidenceId = "";
    private String lastEvent = "initialized";
    private int qaRequestCount;
    private boolean closed;

    public TaskCoordinationWorkflowImpl(
        @Named("policyEvaluationActivitiesStub") final PolicyEvaluationActivities policyEvaluationActivities,
        @Named("codexActivitiesStub") final CodexActivities codexActivities,
        @Named("projectRecordsActivitiesStub") final ProjectRecordsActivities projectRecordsActivities
    ) {
        this.policyEvaluationActivities = policyEvaluationActivities;
        this.codexActivities = codexActivities;
        this.projectRecordsActivities = projectRecordsActivities;
    }

    @Override
    public void run(final TaskWorkflowInput input) {
        this.projectId = normalize(input == null ? "" : input.projectId());
        this.taskId = normalize(input == null ? "" : input.taskId());
        this.lastEvent = "workflow_started";
        while (!closed) {
            Workflow.await(() -> closed || !pendingQaRequests.isEmpty() || !pendingCodexAcceptedSignals.isEmpty() || !pendingSreEnvironmentOutcomes.isEmpty());
            while (!pendingQaRequests.isEmpty()) {
                handleQaRequest(pendingQaRequests.removeFirst());
            }
            while (!pendingCodexAcceptedSignals.isEmpty()) {
                handleCodexAccepted(pendingCodexAcceptedSignals.removeFirst());
            }
            while (!pendingSreEnvironmentOutcomes.isEmpty()) {
                handleSreEnvironmentOutcome(pendingSreEnvironmentOutcomes.removeFirst());
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
    public void reportCodexExecutionAccepted(final CodexExecutionAcceptedSignal accepted) {
        pendingCodexAcceptedSignals.add(normalizedAcceptedSignal(accepted));
        lastEvent = "codex_execution_accept_signal_received";
    }

    @Override
    public void reportSreEnvironmentOutcome(final TaskSreEnvironmentOutcomeSignal outcome) {
        pendingSreEnvironmentOutcomes.add(normalizedOutcome(outcome));
        lastEvent = "sre_environment_signal_received";
    }

    @Override
    public void close(final String reason) {
        closed = true;
        lastEvent = normalize(reason).isBlank() ? "workflow_closed" : normalize(reason);
    }

    @Override
    public TaskWorkflowState currentState() {
        return new TaskWorkflowState(
            projectId,
            taskId,
            workflowStatus,
            taskStatus,
            waitingOn,
            activeBranch,
            environmentStatus,
            environmentName,
            activeExecutionRequestId,
            codexThreadId,
            latestEvidenceId,
            lastEvent,
            qaRequestCount
        );
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
        final String executionRequestId = nextExecutionRequestId();
        final CodexExecutionDispatchResult dispatchResult = codexActivities.dispatchExecution(
            new CodexExecutionDispatchRequest(
                projectId,
                taskId,
                executionRequestId,
                "SRE",
                branchName,
                "runtime/workspaces",
                Workflow.getInfo().getWorkflowId(),
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                normalize(request.requestedByKind()),
                normalize(request.sessionId()),
                "Prepare a branch-scoped compose environment for QA. Use explicit ConstructraOS MCP tools for durable signaling. "
                    + "When you determine the outcome, call constructra_report_sre_environment_outcome exactly once with status ready or failed. "
                    + "If the current bridge/tooling boundary prevents environment work, report status failed with a concise blocker note instead of only describing it conversationally.",
                ""
            )
        );
        projectRecordsActivities.writeExecutionRequest(
            new ProjectExecutionRequestWriteRequest(
                projectId,
                taskId,
                executionRequestId,
                "SRE",
                branchName,
                dispatchResult.status(),
                dispatchResult.codexThreadId(),
                Workflow.getInfo().getWorkflowId(),
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                joinNotes(dispatchResult.note(), request.note())
            )
        );
        final ProjectEvidenceRecord evidenceRecord = projectRecordsActivities.writeEvidence(
            new ProjectEvidenceWriteRequest(
                projectId,
                taskId,
                "qa-request",
                branchName,
                branchRecord.environment(),
                "QA",
                "requested",
                List.of(
                    "QA request accepted by the long-running task workflow.",
                    "Branch and task records were loaded from the repo-backed project contract.",
                    "Codex SRE execution request dispatched for branch environment preparation.",
                    "Awaiting SRE environment preparation and smoke validation."
                ),
                joinNotes(dispatchResult.note(), request.note()),
                normalize(request.requestedByKind()),
                normalize(request.sessionId()),
                Workflow.getInfo().getWorkflowId()
            )
        );
        this.taskStatus = taskRecord.status();
        this.activeBranch = branchName;
        this.waitingOn = "SRE";
        this.environmentStatus = "requested";
        this.environmentName = branchRecord.environment();
        this.activeExecutionRequestId = executionRequestId;
        this.latestEvidenceId = evidenceRecord.id();
        this.qaRequestCount += 1;
        this.lastEvent = "qa_requested";
    }

    private void handleCodexAccepted(final CodexExecutionAcceptedSignal accepted) {
        if (!normalize(accepted.executionRequestId()).equals(activeExecutionRequestId)) {
            return;
        }
        projectRecordsActivities.writeExecutionRequest(
            new ProjectExecutionRequestWriteRequest(
                projectId,
                taskId,
                activeExecutionRequestId,
                normalize(accepted.specialistRole()).isBlank() ? "SRE" : normalize(accepted.specialistRole()),
                activeBranch,
                "accepted",
                normalize(accepted.codexThreadId()),
                Workflow.getInfo().getWorkflowId(),
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                normalize(accepted.note())
            )
        );
        this.codexThreadId = normalize(accepted.codexThreadId());
        this.waitingOn = "SRE";
        this.lastEvent = "codex_execution_accepted";
    }

    private void handleSreEnvironmentOutcome(final TaskSreEnvironmentOutcomeSignal outcome) {
        WorkflowPolicyEnforcer.enforce(
            policyEvaluationActivities,
            "workflow.task.report_sre_environment",
            Map.of(
                "resource_name", "task_workflow",
                "actor", Map.of(
                    "kind", normalize(outcome.reportedByKind()),
                    "session_id", normalize(outcome.sessionId())
                ),
                "request", Map.of(
                    "project_id", projectId,
                    "task_id", taskId,
                    "branch_name", normalize(outcome.branchName()),
                    "status", normalize(outcome.status())
                )
            )
        );
        final ProjectTaskRecord taskRecord = projectRecordsActivities.loadTask(projectId, taskId);
        final String branchName = resolveBranchName(taskRecord, outcome.branchName());
        final String effectiveEnvironmentName = normalize(outcome.environmentName()).isBlank()
            ? environmentName
            : normalize(outcome.environmentName());
        final ProjectEvidenceRecord evidenceRecord = projectRecordsActivities.writeEvidence(
            new ProjectEvidenceWriteRequest(
                projectId,
                taskId,
                "sre-environment-outcome",
                branchName,
                effectiveEnvironmentName,
                "SRE",
                normalize(outcome.status()),
                List.of(
                    "SRE reported a branch-scoped environment outcome back to the task coordination workflow.",
                    "The task workflow recorded the result through the repo-backed evidence boundary."
                ),
                normalize(outcome.note()).isBlank() ? "No additional notes." : normalize(outcome.note()),
                normalize(outcome.reportedByKind()),
                normalize(outcome.sessionId()),
                Workflow.getInfo().getWorkflowId()
            )
        );
        this.taskStatus = taskRecord.status();
        this.activeBranch = branchName;
        this.environmentStatus = normalize(outcome.status());
        this.environmentName = effectiveEnvironmentName;
        this.waitingOn = "ready".equalsIgnoreCase(environmentStatus) ? "QA" : "SRE";
        projectRecordsActivities.writeExecutionRequest(
            new ProjectExecutionRequestWriteRequest(
                projectId,
                taskId,
                activeExecutionRequestId,
                "SRE",
                branchName,
                "ready".equalsIgnoreCase(environmentStatus) ? "completed" : "failed",
                codexThreadId,
                Workflow.getInfo().getWorkflowId(),
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                normalize(outcome.note())
            )
        );
        this.latestEvidenceId = evidenceRecord.id();
        this.lastEvent = "sre_environment_reported";
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

    private static TaskSreEnvironmentOutcomeSignal normalizedOutcome(final TaskSreEnvironmentOutcomeSignal outcome) {
        if (outcome == null) {
            return new TaskSreEnvironmentOutcomeSignal("", "", "", "", "", "");
        }
        return new TaskSreEnvironmentOutcomeSignal(
            normalize(outcome.branchName()),
            normalize(outcome.environmentName()),
            normalize(outcome.status()),
            normalize(outcome.reportedByKind()),
            normalize(outcome.sessionId()),
            normalize(outcome.note())
        );
    }

    private static CodexExecutionAcceptedSignal normalizedAcceptedSignal(final CodexExecutionAcceptedSignal accepted) {
        if (accepted == null) {
            return new CodexExecutionAcceptedSignal("", "", "", "");
        }
        return new CodexExecutionAcceptedSignal(
            normalize(accepted.executionRequestId()),
            normalize(accepted.codexThreadId()),
            normalize(accepted.specialistRole()),
            normalize(accepted.note())
        );
    }

    private String nextExecutionRequestId() {
        final String prefix = taskId + "-exec-";
        int nextSuffix = 1;
        for (final ProjectExecutionRequestRecord record : projectRecordsActivities.listExecutionRequests(projectId, "")) {
            if (!normalize(record.taskId()).equals(taskId)) {
                continue;
            }
            final String executionRequestId = normalize(record.id());
            if (!executionRequestId.startsWith(prefix)) {
                continue;
            }
            final String suffix = executionRequestId.substring(prefix.length());
            try {
                nextSuffix = Math.max(nextSuffix, Integer.parseInt(suffix) + 1);
            } catch (NumberFormatException ignored) {
                // Ignore malformed execution request IDs and continue scanning durable records.
            }
        }
        return prefix + nextSuffix;
    }

    private static String joinNotes(final String first, final String second) {
        final String left = normalize(first);
        final String right = normalize(second);
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + "\n\n" + right;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
