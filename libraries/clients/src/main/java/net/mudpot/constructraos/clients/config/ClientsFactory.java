package net.mudpot.constructraos.clients.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Tracer;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.clients.system.HelloWorldWorkflowClient;

@Factory
public class ClientsFactory {

    @Singleton
    public HelloWorldWorkflowClient helloWorldWorkflowClient(
        @Value("${temporal.address}") final String temporalAddress,
        @Value("${temporal.namespace}") final String temporalNamespace,
        final OpenTelemetry openTelemetry
    ) {
        return new HelloWorldWorkflowClient(
            newWorkflowClient(temporalAddress, temporalNamespace, openTelemetry)
        );
    }

    @Singleton
    public CodexExecutionWorkflowClient codexExecutionWorkflowClient(
        @Value("${temporal.address}") final String temporalAddress,
        @Value("${temporal.namespace}") final String temporalNamespace,
        final OpenTelemetry openTelemetry
    ) {
        return new CodexExecutionWorkflowClient(
            newWorkflowClient(temporalAddress, temporalNamespace, openTelemetry)
        );
    }

    private static WorkflowClient newWorkflowClient(
        final String temporalAddress,
        final String temporalNamespace,
        final OpenTelemetry openTelemetry
    ) {
        final Tracer tracer = OpenTracingShim.createTracerShim(openTelemetry);
        final OpenTracingOptions openTracingOptions = OpenTracingOptions.newBuilder()
            .setTracer(tracer)
            .build();
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
}
