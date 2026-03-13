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

    @Value("${codex.app-server.workspace-root-dir:}")
    private String workspaceRootDir;

    @Value("${codex.app-server.sandbox:workspace-write}")
    private String sandbox;

    public boolean enabled() {
        return enabled;
    }

    public String url() {
        return url;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    public String workspaceRootDir() {
        return workspaceRootDir;
    }

    public String sandbox() {
        return sandbox;
    }
}
