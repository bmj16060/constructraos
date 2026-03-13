package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class CodexBridgeConfig {
    @Value("${codex.bridge.enabled:true}")
    private boolean enabled;

    @Value("${codex.bridge.url:http://codex-bridge:8083/internal/codex}")
    private String url;

    @Value("${codex.bridge.timeout-seconds:10}")
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
