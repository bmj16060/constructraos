package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class KubectlActivityConfig {
    @Value("${kubectl.enabled:false}")
    private boolean enabled;

    @Value("${kubectl.binary:kubectl}")
    private String binary;

    @Value("${kubectl.timeout-seconds:60}")
    private int timeoutSeconds;

    public boolean enabled() {
        return enabled;
    }

    public String binary() {
        return binary;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
