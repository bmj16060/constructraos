package net.mudpot.constructraos.orchestration.project.workflows;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.project.activities.CodexActivities;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionAcceptedSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskQaRequestSignal;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.orchestration.project.workflows.TaskCoordinationWorkflow;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCoordinationWorkflowImplTest {
    @Test
    void requestQaAndSreOutcomeSignalsWriteEvidenceAndUpdateState() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            final CapturingProjectRecordsActivities projectRecordsActivities = new CapturingProjectRecordsActivities();
            environment.newWorker(TaskQueues.TASK_COORDINATION)
                .registerWorkflowImplementationFactory(
                    TaskCoordinationWorkflow.class,
                    () -> new TaskCoordinationWorkflowImpl(new AllowPolicyActivities(), new StubCodexActivities(), projectRecordsActivities)
                );
            environment.start();

            final TaskCoordinationWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                TaskCoordinationWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TaskQueues.TASK_COORDINATION)
                    .setWorkflowId("project-constructraos-task-t-0001")
                    .build()
            );

            WorkflowClient.start(workflow::run, new TaskWorkflowInput("constructraos", "T-0001", "anonymous", "anon-session-1"));
            workflow.requestQa(new TaskQaRequestSignal("", "anonymous", "anon-session-1", "Request the first QA pass."));
            environment.sleep(Duration.ofSeconds(1));
            workflow.reportCodexExecutionAccepted(
                new CodexExecutionAcceptedSignal("T-0001-exec-1", "codex-thread-123", "SRE", "Accepted by Codex.")
            );
            environment.sleep(Duration.ofSeconds(1));
            workflow.reportSreEnvironmentOutcome(
                new net.mudpot.constructraos.commons.orchestration.project.model.TaskSreEnvironmentOutcomeSignal(
                    "project/constructraos/integration",
                    "branch-env-01",
                    "ready",
                    "sre",
                    "anon-session-1",
                    "Environment is healthy for QA."
                )
            );
            environment.sleep(Duration.ofSeconds(1));

            final TaskWorkflowState state = workflow.currentState();

            assertEquals("constructraos", state.projectId());
            assertEquals("T-0001", state.taskId());
            assertEquals("project/constructraos/integration", state.activeBranch());
            assertEquals("E-0002", state.latestEvidenceId());
            assertEquals("QA", state.waitingOn());
            assertEquals("ready", state.environmentStatus());
            assertEquals("branch-env-01", state.environmentName());
            assertEquals("T-0001-exec-1", state.activeExecutionRequestId());
            assertEquals("codex-thread-123", state.codexThreadId());
            assertEquals(1, state.qaRequestCount());
            assertEquals("project/constructraos/integration", projectRecordsActivities.latestRequest.branchName());
            assertEquals("sre-environment-outcome", projectRecordsActivities.latestRequest.evidenceType());
            assertEquals("T-0001-exec-1", projectRecordsActivities.latestExecutionRequest.executionRequestId());
            workflow.close("test-complete");
        }
    }

    private static final class CapturingProjectRecordsActivities implements ProjectRecordsActivities {
        private ProjectEvidenceWriteRequest latestRequest;
        private ProjectExecutionRequestWriteRequest latestExecutionRequest;
        private int evidenceCount;

        @Override
        public ProjectTaskRecord loadTask(final String projectId, final String taskId) {
            return new ProjectTaskRecord(
                projectId,
                taskId,
                "Bootstrap project filesystem contract",
                "in_progress",
                "PM",
                "project/constructraos/integration",
                List.of(),
                List.of(),
                List.of(),
                "/tmp/task.md"
            );
        }

        @Override
        public ProjectBranchRecord loadBranch(final String projectId, final String branchName) {
            return new ProjectBranchRecord(projectId, branchName, "project control branch", "ConstructraOS project roll-up branch", "planned integration environment", "planned");
        }

        @Override
        public ProjectEvidenceRecord writeEvidence(final ProjectEvidenceWriteRequest request) {
            this.latestRequest = request;
            this.evidenceCount += 1;
            return new ProjectEvidenceRecord(
                "E-000" + evidenceCount,
                "/tmp/E-000" + evidenceCount + ".md",
                request.projectId(),
                request.taskId(),
                request.branchName(),
                request.environment(),
                request.status(),
                request.validatingSpecialist(),
                "2026-03-13T00:00:00Z"
            );
        }

        @Override
        public ProjectExecutionRequestRecord writeExecutionRequest(final ProjectExecutionRequestWriteRequest request) {
            this.latestExecutionRequest = request;
            return new ProjectExecutionRequestRecord(
                request.executionRequestId(),
                "/tmp/" + request.executionRequestId() + ".md",
                request.projectId(),
                request.taskId(),
                request.specialistRole(),
                request.branchName(),
                request.status(),
                request.codexThreadId(),
                request.workflowId()
            );
        }
    }

    private static final class StubCodexActivities implements CodexActivities {
        @Override
        public CodexExecutionDispatchResult dispatchExecution(final CodexExecutionDispatchRequest request) {
            return new CodexExecutionDispatchResult(
                request.executionRequestId(),
                "",
                "dispatched",
                "Codex request dispatched."
            );
        }
    }

    private static final class AllowPolicyActivities implements PolicyEvaluationActivities {
        @Override
        public PolicyEvaluationResult evaluatePolicy(final PolicyEvaluationRequest request) {
            return new PolicyEvaluationResult(true, "allowed", "constructraos.v1");
        }
    }
}
