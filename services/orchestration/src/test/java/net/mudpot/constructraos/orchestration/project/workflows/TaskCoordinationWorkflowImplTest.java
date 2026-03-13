package net.mudpot.constructraos.orchestration.project.workflows;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.project.activities.ProjectRecordsActivities;
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
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCoordinationWorkflowImplTest {
    @Test
    void requestQaSignalWritesEvidenceAndUpdatesState() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            final CapturingProjectRecordsActivities projectRecordsActivities = new CapturingProjectRecordsActivities();
            environment.newWorker(TaskQueues.TASK_COORDINATION)
                .registerWorkflowImplementationFactory(
                    TaskCoordinationWorkflow.class,
                    () -> new TaskCoordinationWorkflowImpl(new AllowPolicyActivities(), projectRecordsActivities)
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

            final TaskWorkflowState state = workflow.currentState();

            assertEquals("constructraos", state.projectId());
            assertEquals("T-0001", state.taskId());
            assertEquals("project/constructraos/integration", state.activeBranch());
            assertEquals("E-0001", state.latestEvidenceId());
            assertEquals(1, state.qaRequestCount());
            assertEquals("project/constructraos/integration", projectRecordsActivities.latestRequest.branchName());
            workflow.close("test-complete");
        }
    }

    private static final class CapturingProjectRecordsActivities implements ProjectRecordsActivities {
        private ProjectEvidenceWriteRequest latestRequest;

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
        public ProjectEvidenceRecord writeQaEvidence(final ProjectEvidenceWriteRequest request) {
            this.latestRequest = request;
            return new ProjectEvidenceRecord(
                "E-0001",
                "/tmp/E-0001.md",
                request.projectId(),
                request.taskId(),
                request.branchName(),
                request.environment(),
                request.status(),
                request.validatingSpecialist(),
                "2026-03-13T00:00:00Z"
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
