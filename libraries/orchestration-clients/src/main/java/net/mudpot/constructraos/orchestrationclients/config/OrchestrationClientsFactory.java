package net.mudpot.constructraos.orchestrationclients.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.orchestrationclients.system.HelloWorldWorkflowClient;
import net.mudpot.constructraos.orchestrationclients.project.TaskCoordinationWorkflowClient;

@Factory
public class OrchestrationClientsFactory {

    @Singleton
    public WorkflowClient workflowClient(
        @Value("${temporal.address}") final String temporalAddress,
        @Value("${temporal.namespace}") final String temporalNamespace,
        final OpenTracingOptions openTracingOptions
    ) {
        final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder().setTarget(temporalAddress).build()
        );
        return WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setNamespace(temporalNamespace)
                .setInterceptors(new OpenTracingClientInterceptor(openTracingOptions))
                .build()
        );
    }

    @Singleton
    public HelloWorldWorkflowClient helloWorldWorkflowClient(final WorkflowClient workflowClient) {
        return new HelloWorldWorkflowClient(workflowClient);
    }

    @Singleton
    public TaskCoordinationWorkflowClient taskCoordinationWorkflowClient(final WorkflowClient workflowClient) {
        return new TaskCoordinationWorkflowClient(workflowClient);
    }
}
