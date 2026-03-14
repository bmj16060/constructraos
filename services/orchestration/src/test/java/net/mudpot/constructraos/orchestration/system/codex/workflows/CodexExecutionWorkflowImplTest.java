package net.mudpot.constructraos.orchestration.system.codex.workflows;

import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.codex.activities.CodexExecutionActivities;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.codex.workflows.CodexExecutionWorkflow;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexExecutionWorkflowImplTest {
    @Test
    void runUsesCodexExecutionActivity() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            environment.newWorker(TaskQueues.CODEX_EXECUTION)
                .registerWorkflowImplementationFactory(
                    CodexExecutionWorkflow.class,
                    () -> new CodexExecutionWorkflowImpl(new StubCodexExecutionActivities(), new AllowPolicyActivities())
                );
            environment.start();

            final CodexExecutionWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                CodexExecutionWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TaskQueues.CODEX_EXECUTION)
                    .setWorkflowId("wf-codex")
                    .build()
            );

            final CodexExecutionResult result = workflow.run(
                new CodexExecutionWorkflowInput(
                    "Return a minimal structured update for task-001.",
                    "/tmp/project",
                    "planner",
                    "anonymous",
                    "anon-session-1"
                )
            );

            assertEquals("completed", result.status());
            assertEquals("wf-codex", StubCodexExecutionActivities.lastWorkflowId);
            assertEquals("planner", StubCodexExecutionActivities.lastAgentName);
            assertEquals("anonymous", StubCodexExecutionActivities.lastActorKind);
            assertEquals("anon-session-1", StubCodexExecutionActivities.lastSessionId);
        }
    }

    @Test
    void runRejectsWhenWorkflowPolicyDenies() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            environment.newWorker(TaskQueues.CODEX_EXECUTION)
                .registerWorkflowImplementationFactory(
                    CodexExecutionWorkflow.class,
                    () -> new CodexExecutionWorkflowImpl(new StubCodexExecutionActivities(), new DenyPolicyActivities())
                );
            environment.start();

            final CodexExecutionWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                CodexExecutionWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TaskQueues.CODEX_EXECUTION)
                    .setWorkflowId("wf-codex-denied")
                    .build()
            );

            final WorkflowFailedException exception = assertThrows(
                WorkflowFailedException.class,
                () -> workflow.run(
                    new CodexExecutionWorkflowInput("Too short", "/tmp/project", "planner", "anonymous", "anon-session-1")
                )
            );

            final ApplicationFailure cause = (ApplicationFailure) exception.getCause();
            assertEquals("workflow_policy_denied:codex_execution_prompt_too_short", cause.getType());
        }
    }

    private static final class StubCodexExecutionActivities implements CodexExecutionActivities {
        private static String lastWorkflowId;
        private static String lastAgentName;
        private static String lastActorKind;
        private static String lastSessionId;

        @Override
        public CodexExecutionResult execute(final CodexExecutionActivityInput input) {
            lastWorkflowId = input.workflowId();
            lastAgentName = input.agentName();
            lastActorKind = input.actorKind();
            lastSessionId = input.sessionId();
            return new CodexExecutionResult("completed", "Codex returned the structured result.", "none");
        }
    }

    private static final class AllowPolicyActivities implements PolicyEvaluationActivities {
        @Override
        public PolicyEvaluationResult evaluatePolicy(final PolicyEvaluationRequest request) {
            return new PolicyEvaluationResult(true, "allowed", "constructraos.v1");
        }
    }

    private static final class DenyPolicyActivities implements PolicyEvaluationActivities {
        @Override
        public PolicyEvaluationResult evaluatePolicy(final PolicyEvaluationRequest request) {
            return new PolicyEvaluationResult(false, "codex_execution_prompt_too_short", "constructraos.v1");
        }
    }
}
