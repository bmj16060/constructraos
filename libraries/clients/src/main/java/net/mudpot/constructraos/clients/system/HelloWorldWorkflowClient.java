package net.mudpot.constructraos.clients.system;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;
import net.mudpot.constructraos.commons.orchestration.WorkflowNames;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldWorkflowInput;
import net.mudpot.constructraos.commons.orchestration.system.workflows.HelloWorldWorkflow;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;

import java.util.UUID;

public class HelloWorldWorkflowClient {
    private final WorkflowClient workflowClient;

    public HelloWorldWorkflowClient(final WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public WorkflowStartResponse start(final String name, final String workflowId, final String actorKind, final String sessionId) {
        final String resolvedId = (workflowId == null || workflowId.isBlank())
            ? "hello-world-" + UUID.randomUUID()
            : workflowId;
        final HelloWorldWorkflow workflow = workflowStub(resolvedId);
        final WorkflowExecution execution = WorkflowClient.start(workflow::run, workflowInput(name, null, actorKind, sessionId));
        return new WorkflowStartResponse(
            WorkflowNames.HELLO_WORLD,
            execution.getWorkflowId(),
            TaskQueues.HELLO_WORLD,
            execution.getRunId()
        );
    }

    public WorkflowStartResponse start(final String name, final String useCase, final String workflowId, final String actorKind, final String sessionId) {
        final String resolvedId = (workflowId == null || workflowId.isBlank())
            ? "hello-world-" + UUID.randomUUID()
            : workflowId;
        final HelloWorldWorkflow workflow = workflowStub(resolvedId);
        final WorkflowExecution execution = WorkflowClient.start(workflow::run, workflowInput(name, useCase, actorKind, sessionId));
        return new WorkflowStartResponse(
            WorkflowNames.HELLO_WORLD,
            execution.getWorkflowId(),
            TaskQueues.HELLO_WORLD,
            execution.getRunId()
        );
    }

    public HelloWorldResult run(final String name, final String useCase, final String actorKind, final String sessionId) {
        return workflowStub("hello-world-" + UUID.randomUUID()).run(workflowInput(name, useCase, actorKind, sessionId));
    }

    private HelloWorldWorkflow workflowStub(final String workflowId) {
        return workflowClient.newWorkflowStub(
            HelloWorldWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TaskQueues.HELLO_WORLD)
                .setWorkflowId(workflowId)
                .build()
        );
    }

    private static String normalizedName(final String name) {
        final String value = name == null ? "" : name.trim();
        return value.isBlank() ? "World" : value;
    }

    private static String normalizedUseCase(final String useCase) {
        final String value = useCase == null ? "" : useCase.trim();
        return value.isBlank() ? "Demonstrate the ConstructraOS platform baseline." : value;
    }

    private static HelloWorldWorkflowInput workflowInput(
        final String name,
        final String useCase,
        final String actorKind,
        final String sessionId
    ) {
        return new HelloWorldWorkflowInput(
            normalizedName(name),
            normalizedUseCase(useCase),
            actorKind == null ? "" : actorKind.trim(),
            sessionId == null ? "" : sessionId.trim()
        );
    }
}
