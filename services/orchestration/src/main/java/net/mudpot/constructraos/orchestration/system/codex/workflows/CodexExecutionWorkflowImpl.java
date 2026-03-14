package net.mudpot.constructraos.orchestration.system.codex.workflows;

import io.micronaut.context.annotation.Prototype;
import io.temporal.workflow.Workflow;
import jakarta.inject.Named;
import net.mudpot.constructraos.commons.orchestration.codex.activities.CodexExecutionActivities;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionActivityInput;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.codex.workflows.CodexExecutionWorkflow;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.orchestration.core.policy.WorkflowPolicyEnforcer;

import java.util.Map;

@Prototype
public class CodexExecutionWorkflowImpl implements CodexExecutionWorkflow {
    private final CodexExecutionActivities codexExecutionActivities;
    private final PolicyEvaluationActivities policyEvaluationActivities;

    public CodexExecutionWorkflowImpl(
        @Named("codexExecutionActivitiesStub") final CodexExecutionActivities codexExecutionActivities,
        @Named("policyEvaluationActivitiesStub") final PolicyEvaluationActivities policyEvaluationActivities
    ) {
        this.codexExecutionActivities = codexExecutionActivities;
        this.policyEvaluationActivities = policyEvaluationActivities;
    }

    @Override
    public CodexExecutionResult run(final CodexExecutionWorkflowInput input) {
        final String prompt = requiredValue(input == null ? null : input.prompt(), "Codex execution prompt is required.");
        final String workingDirectory = optionalValue(input == null ? null : input.workingDirectory());
        final String agentName = optionalValue(input == null ? null : input.agentName()).isBlank()
            ? "planner"
            : optionalValue(input.agentName());

        WorkflowPolicyEnforcer.enforce(
            policyEvaluationActivities,
            "workflow.codex_execution.execute",
            Map.of(
                "resource_name", "workflow",
                "actor", Map.of(
                    "kind", optionalValue(input == null ? null : input.actorKind()),
                    "session_id", optionalValue(input == null ? null : input.sessionId())
                ),
                "request", Map.of(
                    "prompt", prompt,
                    "working_directory", workingDirectory,
                    "agent_name", agentName
                )
            )
        );

        return codexExecutionActivities.execute(
            new CodexExecutionActivityInput(
                Workflow.getInfo().getWorkflowId(),
                prompt,
                workingDirectory,
                agentName,
                optionalValue(input == null ? null : input.actorKind()),
                optionalValue(input == null ? null : input.sessionId())
            )
        );
    }

    private static String requiredValue(final String value, final String message) {
        final String normalized = optionalValue(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String optionalValue(final String value) {
        return value == null ? "" : value.trim();
    }
}
