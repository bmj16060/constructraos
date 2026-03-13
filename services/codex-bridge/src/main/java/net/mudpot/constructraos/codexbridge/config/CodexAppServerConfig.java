package net.mudpot.constructraos.codexbridge.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class CodexAppServerConfig {
    @Value("${codex.app-server.enabled:false}")
    private boolean enabled;

    @Value("${codex.app-server.url:}")
    private String url;

    @Value("${codex.app-server.timeout-seconds:10}")
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
