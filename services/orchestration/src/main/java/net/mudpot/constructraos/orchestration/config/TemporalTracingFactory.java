package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Factory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Tracer;
import io.temporal.opentracing.OpenTracingOptions;
import jakarta.inject.Singleton;

@Factory
public class TemporalTracingFactory {

    @Singleton
    public OpenTracingOptions temporalOpenTracingOptions(final OpenTelemetry openTelemetry) {
        final Tracer tracer = OpenTracingShim.createTracerShim(openTelemetry);
        return OpenTracingOptions.newBuilder()
            .setTracer(tracer)
            .build();
    }
}
