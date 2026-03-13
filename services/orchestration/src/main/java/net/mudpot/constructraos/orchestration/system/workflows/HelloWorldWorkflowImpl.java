package net.mudpot.constructraos.orchestration.system.workflows;

import io.micronaut.context.annotation.Prototype;
import io.temporal.workflow.Workflow;
import jakarta.inject.Named;
import net.mudpot.constructraos.commons.ai.model.LlmResponse;
import net.mudpot.constructraos.commons.ai.model.PromptBundle;
import net.mudpot.constructraos.commons.orchestration.ai.activities.LlmActivities;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.orchestration.ai.activities.PromptActivities;
import net.mudpot.constructraos.commons.orchestration.system.activities.HelloActivities;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.system.workflows.HelloWorldWorkflow;
import net.mudpot.constructraos.orchestration.core.policy.WorkflowPolicyEnforcer;

import java.util.Map;

@Prototype
public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {
    private final HelloActivities helloActivities;
    private final PromptActivities promptActivities;
    private final LlmActivities llmActivities;
    private final PolicyEvaluationActivities policyEvaluationActivities;

    public HelloWorldWorkflowImpl(
        @Named("helloActivitiesStub") final HelloActivities helloActivities,
        @Named("promptActivitiesStub") final PromptActivities promptActivities,
        @Named("llmActivitiesStub") final LlmActivities llmActivities,
        @Named("policyEvaluationActivitiesStub") final PolicyEvaluationActivities policyEvaluationActivities
    ) {
        this.helloActivities = helloActivities;
        this.promptActivities = promptActivities;
        this.llmActivities = llmActivities;
        this.policyEvaluationActivities = policyEvaluationActivities;
    }

    @Override
    public HelloWorldResult run(final HelloWorldWorkflowInput input) {
        final String normalizedName = input == null || input.name() == null || input.name().isBlank() ? "World" : input.name().trim();
        final String normalizedUseCase = input == null || input.useCase() == null || input.useCase().isBlank()
            ? "Demonstrate the ConstructraOS platform baseline."
            : input.useCase().trim();
        WorkflowPolicyEnforcer.enforce(
            policyEvaluationActivities,
            "workflow.hello_world.execute",
            Map.of(
                "resource_name", "workflow",
                "actor", Map.of(
                    "kind", input == null || input.actorKind() == null ? "" : input.actorKind().trim(),
                    "session_id", input == null || input.sessionId() == null ? "" : input.sessionId().trim()
                ),
                "request", Map.of(
                    "name", normalizedName,
                    "use_case", normalizedUseCase
                )
            )
        );
        final PromptBundle prompt = promptActivities.renderPrompt("starter_hello_v1", Map.of(
            "name", normalizedName,
            "use_case", normalizedUseCase
        ));
        final LlmResponse llmResponse = llmActivities.callLlm(
            prompt.getUserPrompt(),
            null,
            null,
            prompt.getSystemPrompt(),
            0.3,
            500,
            "hello:" + normalizedName.toLowerCase() + ":" + Integer.toHexString(normalizedUseCase.hashCode()),
            300,
            false
        );
        return helloActivities.completeHello(
            Workflow.getInfo().getWorkflowId(),
            normalizedName,
            normalizedUseCase,
            prompt,
            llmResponse
        );
    }
}
