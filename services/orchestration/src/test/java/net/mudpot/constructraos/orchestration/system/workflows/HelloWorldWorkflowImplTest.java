package net.mudpot.constructraos.orchestration.system.workflows;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import net.mudpot.constructraos.commons.ai.model.LlmResponse;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.ai.activities.LlmActivities;
import net.mudpot.constructraos.commons.orchestration.ai.activities.PromptActivities;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.orchestration.system.activities.HelloActivities;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.system.workflows.HelloWorldWorkflow;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelloWorldWorkflowImplTest {
    @Test
    void runUsesPromptAndLlmActivities() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            environment.newWorker(TaskQueues.HELLO_WORLD)
                .registerWorkflowImplementationFactory(
                    HelloWorldWorkflow.class,
                    () -> new HelloWorldWorkflowImpl(
                        new StubHelloActivities(),
                        new StubPromptActivities(),
                        new StubLlmActivities(),
                        new AllowPolicyActivities()
                    )
                );
            environment.start();

            final HelloWorldWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                HelloWorldWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TaskQueues.HELLO_WORLD)
                    .setWorkflowId("wf-hello")
                    .build()
            );

            final HelloWorldResult result = workflow.run(
                new HelloWorldWorkflowInput("Brandon", "Build a flight-club platform.", "anonymous", "anon-session-1")
            );

            assertEquals("wf-hello", result.workflowId());
            assertEquals("Hello from the LLM.", result.greeting());
            assertEquals("starter_hello_v1", result.promptTemplate());
        }
    }

    @Test
    void runRejectsWhenWorkflowPolicyDenies() {
        try (TestWorkflowEnvironment environment = TestWorkflowEnvironment.newInstance()) {
            environment.newWorker(TaskQueues.HELLO_WORLD)
                .registerWorkflowImplementationFactory(
                    HelloWorldWorkflow.class,
                    () -> new HelloWorldWorkflowImpl(
                        new StubHelloActivities(),
                        new StubPromptActivities(),
                        new StubLlmActivities(),
                        new DenyPolicyActivities()
                    )
                );
            environment.start();

            final HelloWorldWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                HelloWorldWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setTaskQueue(TaskQueues.HELLO_WORLD)
                    .setWorkflowId("wf-policy-denied")
                    .build()
            );

            final WorkflowFailedException exception = assertThrows(
                WorkflowFailedException.class,
                () -> workflow.run(new HelloWorldWorkflowInput("Brandon", "Too short", "anonymous", "anon-session-1"))
            );

            final ApplicationFailure cause = (ApplicationFailure) exception.getCause();
            assertEquals("workflow_policy_denied:hello_world_use_case_too_short", cause.getType());
        }
    }

    private static final class StubHelloActivities implements HelloActivities {
        @Override
        public HelloWorldResult completeHello(
            final String workflowId,
            final String name,
            final String useCase,
            final PromptBundle prompt,
            final LlmResponse llmResponse
        ) {
            return new HelloWorldResult(
                workflowId,
                prompt.getTemplate(),
                llmResponse.getText(),
                llmResponse.getProvider(),
                llmResponse.getModel(),
                llmResponse.getUsage(),
                llmResponse.getCache(),
                Instant.parse("2026-03-12T00:00:00Z")
            );
        }
    }

    private static final class StubPromptActivities implements PromptActivities {
        @Override
        public PromptBundle renderPrompt(final String templateName, final Map<String, Object> variables) {
            return new PromptBundle(templateName, "system", "user", variables);
        }
    }

    private static final class StubLlmActivities implements LlmActivities {
        @Override
        public LlmResponse callLlm(
            final String userPrompt,
            final String provider,
            final String model,
            final String systemPrompt,
            final double temperature,
            final int maxTokens,
            final String cacheKey,
            final int cacheTtlSeconds,
            final boolean cacheByPromptHash
        ) {
            return new LlmResponse("openai-compatible", "demo", "Hello from the LLM.", Map.of(), Map.of(), Map.of("hit", false));
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
            return new PolicyEvaluationResult(false, "hello_world_use_case_too_short", "constructraos.v1");
        }
    }
}
