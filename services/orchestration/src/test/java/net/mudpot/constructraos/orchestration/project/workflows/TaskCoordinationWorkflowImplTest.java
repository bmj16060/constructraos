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
import net.mudpot.constructraos.orchestration.project.activities.ProjectRecordsActivitiesImpl;
import net.mudpot.constructraos.projectrecords.FilesystemProjectRecordsGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCoordinationWorkflowImplTest {
    @TempDir
    Path tempDir;

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

    @Test
    void workflowPersistsRealExecutionAndEvidenceRecords() throws IOException {
        final Path projectRoot = seedProjectTree();
        final FilesystemProjectRecordsGateway gateway = new FilesystemProjectRecordsGateway(projectRoot.getParent().toString());
        final ProjectRecordsActivitiesImpl projectRecordsActivities = new ProjectRecordsActivitiesImpl(gateway);

        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
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
            workflow.requestQa(new TaskQaRequestSignal("", "anonymous", "anon-session-1", "Real records QA request."));
            environment.sleep(Duration.ofSeconds(1));
            workflow.reportCodexExecutionAccepted(
                new CodexExecutionAcceptedSignal("T-0001-exec-1", "codex-thread-123", "SRE", "Accepted.")
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
            final String executionIndex = Files.readString(projectRoot.resolve("executions").resolve("index.md"));
            final String evidenceIndex = Files.readString(projectRoot.resolve("evidence").resolve("index.md"));
            final String taskRecord = Files.readString(projectRoot.resolve("tasks").resolve("T-0001-bootstrap-project-contract.md"));

            assertEquals("T-0001-exec-1", state.activeExecutionRequestId());
            assertEquals("codex-thread-123", state.codexThreadId());
            assertEquals("ready", state.environmentStatus());
            assertTrue(executionIndex.contains("T-0001-exec-1"));
            assertTrue(executionIndex.contains("completed"));
            assertTrue(executionIndex.contains("codex-thread-123"));
            assertTrue(evidenceIndex.contains("E-0001"));
            assertTrue(evidenceIndex.contains("E-0002"));
            assertTrue(taskRecord.contains("E-0001"));
            assertTrue(taskRecord.contains("E-0002"));

            workflow.close("test-complete");
        }
    }

    @Test
    void requestQaUsesNextDurableExecutionRequestIdWhenPriorRequestsExist() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            final CapturingProjectRecordsActivities projectRecordsActivities = new CapturingProjectRecordsActivities();
            projectRecordsActivities.executionRequests.add(
                new ProjectExecutionRequestRecord(
                    "T-0001-exec-1",
                    "/tmp/T-0001-exec-1.md",
                    "constructraos",
                    "T-0001",
                    "SRE",
                    "project/constructraos/integration",
                    "failed",
                    "codex-thread-old",
                    "project-constructraos-task-t-0001",
                    "reportCodexExecutionAccepted",
                    "reportSreEnvironmentOutcome",
                    "Previous attempt."
                )
            );
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
            workflow.requestQa(new TaskQaRequestSignal("", "anonymous", "anon-session-1", "Retry QA pass."));
            environment.sleep(Duration.ofSeconds(1));

            final TaskWorkflowState state = workflow.currentState();

            assertEquals("T-0001-exec-2", state.activeExecutionRequestId());
            assertEquals("T-0001-exec-2", projectRecordsActivities.latestExecutionRequest.executionRequestId());
            workflow.close("test-complete");
        }
    }

    @Test
    void reportSreOutcomePreservesCurrentEnvironmentNameWhenSignalOmitsIt() {
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
                    "",
                    "failed",
                    "sre",
                    "anon-session-1",
                    "Bridge fallback failure."
                )
            );
            environment.sleep(Duration.ofSeconds(1));

            final TaskWorkflowState state = workflow.currentState();

            assertEquals("planned integration environment", state.environmentName());
            assertEquals("planned integration environment", projectRecordsActivities.latestRequest.environment());
            workflow.close("test-complete");
        }
    }

    private static final class CapturingProjectRecordsActivities implements ProjectRecordsActivities {
        private ProjectEvidenceWriteRequest latestRequest;
        private ProjectExecutionRequestWriteRequest latestExecutionRequest;
        private int evidenceCount;
        private final List<ProjectExecutionRequestRecord> executionRequests = new ArrayList<>();

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
            final ProjectExecutionRequestRecord record = new ProjectExecutionRequestRecord(
                request.executionRequestId(),
                "/tmp/" + request.executionRequestId() + ".md",
                request.projectId(),
                request.taskId(),
                request.specialistRole(),
                request.branchName(),
                request.status(),
                request.codexThreadId(),
                request.workflowId(),
                request.callbackSignal(),
                request.callbackFailureSignal(),
                request.note()
            );
            executionRequests.removeIf(existing -> existing.id().equals(record.id()));
            executionRequests.add(record);
            return record;
        }

        @Override
        public List<ProjectExecutionRequestRecord> listExecutionRequests(final String projectId, final String status) {
            return executionRequests.stream()
                .filter(record -> record.projectId().equals(projectId))
                .filter(record -> status == null || status.isBlank() || record.status().equalsIgnoreCase(status))
                .toList();
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

    private Path seedProjectTree() throws IOException {
        final Path projectsRoot = tempDir.resolve("projects");
        final Path projectRoot = projectsRoot.resolve("constructraos");
        Files.createDirectories(projectRoot.resolve("tasks"));
        Files.createDirectories(projectRoot.resolve("branches"));
        Files.createDirectories(projectRoot.resolve("evidence"));
        Files.createDirectories(projectRoot.resolve("executions"));
        Files.writeString(
            projectRoot.resolve("tasks").resolve("T-0001-bootstrap-project-contract.md"),
            """
            # T-0001: Bootstrap project filesystem contract

            - Status: in_progress
            - Owning specialist: PM
            - Parent control branch: `project/constructraos/integration`
            - Specialist branches: none yet
            - Linked ADRs:
              - [ADR-0001](/tmp/adr)
            - Linked bugs: none
            - Latest evidence: none
            """
        );
        Files.writeString(
            projectRoot.resolve("branches").resolve("index.md"),
            """
            # Branch Index

            | Branch | Role | Scope | Environment | Status |
            | --- | --- | --- | --- | --- |
            | `project/constructraos/integration` | project control branch | ConstructraOS project roll-up branch | planned integration environment | planned |
            """
        );
        Files.writeString(projectRoot.resolve("evidence").resolve("index.md"), "# Evidence Index\n\nNo QA or test evidence has been recorded yet.\n");
        Files.writeString(projectRoot.resolve("executions").resolve("index.md"), "# Execution Request Index\n\nNo specialist execution requests have been recorded yet.\n");
        return projectRoot;
    }
}
