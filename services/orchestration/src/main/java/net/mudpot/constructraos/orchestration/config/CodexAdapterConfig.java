package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class CodexAdapterConfig {
    @Value("${codex.adapter.enabled:false}")
    private boolean enabled;

    @Value("${codex.adapter.url:}")
    private String url;

    @Value("${codex.adapter.timeout-seconds:10}")
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
