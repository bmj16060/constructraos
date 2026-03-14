package net.mudpot.constructraos.clients.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.clients.system.HelloWorldWorkflowClient;

@Factory
public class ClientsFactory {

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
}
