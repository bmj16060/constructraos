package net.mudpot.constructraos.orchestration.config;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class TemporalWorkerConfig {

    @Value("${temporal.address}")
    private String temporalAddress;

    @Value("${temporal.namespace}")
    private String temporalNamespace;

    @Value("${temporal.task-queue}")
    private String helloTaskQueue;

    @Value("${temporal.codex-task-queue}")
    private String codexTaskQueue;

    @Value("${ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${ai.openai.default-model}")
    private String openAiDefaultModel;

    @Value("${ai.openai.max-retries:0}")
    private int openAiMaxRetries;

    @Value("${ai.default-provider}")
    private String aiDefaultProvider;

    @Value("${ai.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${ai.anthropic.base-url}")
    private String anthropicBaseUrl;

    @Value("${ai.anthropic.api-version}")
    private String anthropicApiVersion;

    @Value("${ai.anthropic.default-model}")
    private String anthropicDefaultModel;

    @Value("${ai.anthropic.max-retries:0}")
    private int anthropicMaxRetries;

    @Value("${valkey.enabled}")
    private boolean valkeyEnabled;

    @Value("${valkey.host}")
    private String valkeyHost;

    @Value("${valkey.port}")
    private int valkeyPort;

    public String temporalAddress() { return temporalAddress; }
    public String temporalNamespace() { return temporalNamespace; }
    public String helloTaskQueue() { return helloTaskQueue; }
    public String codexTaskQueue() { return codexTaskQueue; }
    public String openAiBaseUrl() { return openAiBaseUrl; }
    public String openAiApiKey() { return openAiApiKey; }
    public String openAiDefaultModel() { return openAiDefaultModel; }
    public int openAiMaxRetries() { return openAiMaxRetries; }
    public String aiDefaultProvider() { return aiDefaultProvider; }
    public String anthropicApiKey() { return anthropicApiKey; }
    public String anthropicBaseUrl() { return anthropicBaseUrl; }
    public String anthropicApiVersion() { return anthropicApiVersion; }
    public String anthropicDefaultModel() { return anthropicDefaultModel; }
    public int anthropicMaxRetries() { return anthropicMaxRetries; }
    public boolean valkeyEnabled() { return valkeyEnabled; }
    public String valkeyHost() { return valkeyHost; }
    public int valkeyPort() { return valkeyPort; }
}
