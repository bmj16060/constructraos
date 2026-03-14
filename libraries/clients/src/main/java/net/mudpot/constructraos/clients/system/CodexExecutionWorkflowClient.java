package net.mudpot.constructraos.clients.system;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.WorkflowNames;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.codex.workflows.CodexExecutionWorkflow;

import java.nio.file.Paths;
import java.util.UUID;

public class CodexExecutionWorkflowClient {
    private static final String DEFAULT_AGENT_NAME = "planner";

    private final WorkflowClient workflowClient;

    public CodexExecutionWorkflowClient(final WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public WorkflowStartResponse start(
        final String prompt,
        final String workingDirectory,
        final String agentName,
        final String workflowId,
        final String actorKind,
        final String sessionId
    ) {
        final String resolvedId = resolvedWorkflowId(workflowId);
        final CodexExecutionWorkflow workflow = workflowStub(resolvedId);
        final WorkflowExecution execution = WorkflowClient.start(
            workflow::run,
            workflowInput(prompt, workingDirectory, agentName, actorKind, sessionId)
        );
        return new WorkflowStartResponse(
            WorkflowNames.CODEX_EXECUTION,
            execution.getWorkflowId(),
            TaskQueues.CODEX_EXECUTION,
            execution.getRunId()
        );
    }

    public CodexExecutionResult run(
        final String prompt,
        final String workingDirectory,
        final String agentName,
        final String actorKind,
        final String sessionId
    ) {
        return workflowStub(resolvedWorkflowId("")).run(workflowInput(prompt, workingDirectory, agentName, actorKind, sessionId));
    }

    private CodexExecutionWorkflow workflowStub(final String workflowId) {
        return workflowClient.newWorkflowStub(
            CodexExecutionWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueues.CODEX_EXECUTION)
                .setWorkflowId(workflowId)
                .build()
        );
    }

    private static String resolvedWorkflowId(final String workflowId) {
        final String normalized = sanitize(workflowId);
        return normalized.isBlank() ? "codex-execution-" + UUID.randomUUID() : normalized;
    }

    private static CodexExecutionWorkflowInput workflowInput(
        final String prompt,
        final String workingDirectory,
        final String agentName,
        final String actorKind,
        final String sessionId
    ) {
        return new CodexExecutionWorkflowInput(
            normalizedPrompt(prompt),
            normalizedWorkingDirectory(workingDirectory),
            normalizedAgentName(agentName),
            sanitize(actorKind),
            sanitize(sessionId)
        );
    }

    private static String normalizedPrompt(final String prompt) {
        final String normalized = sanitize(prompt);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Codex execution prompt is required.");
        }
        return normalized;
    }

    private static String normalizedWorkingDirectory(final String workingDirectory) {
        final String normalized = sanitize(workingDirectory);
        return normalized.isBlank()
            ? Paths.get("").toAbsolutePath().normalize().toString()
            : normalized;
    }

    private static String normalizedAgentName(final String agentName) {
        final String normalized = sanitize(agentName);
        return normalized.isBlank() ? DEFAULT_AGENT_NAME : normalized;
    }

    private static String sanitize(final String value) {
        return value == null ? "" : value.trim();
    }
}
