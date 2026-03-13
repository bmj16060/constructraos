package net.mudpot.constructraos.codexbridge.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class ConstructraApiConfig {
    @Value("${constructra.api.enabled:false}")
    private boolean enabled;

    @Value("${constructra.api.url:}")
    private String url;

    @Value("${constructra.api.timeout-seconds:10}")
    private int timeoutSeconds;

    public boolean enabled() {
        return enabled;
    }

    public String url() {
        return url;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
