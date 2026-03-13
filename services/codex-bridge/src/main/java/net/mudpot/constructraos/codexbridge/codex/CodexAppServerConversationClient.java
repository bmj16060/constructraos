package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.config.CodexAppServerConfig;

@Singleton
public class CodexAppServerConversationClient implements CodexConversationClient {
    private final CodexAppServerConfig config;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public CodexAppServerConversationClient(final CodexAppServerConfig config, final ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public CodexExecutionDispatchResult dispatch(final CodexExecutionDispatchRequest request) {
        if (!config.enabled() || config.url().isBlank()) {
            return new CodexExecutionDispatchResult(
                request.executionRequestId(),
                "",
                "dispatched",
                "Codex App Server is not configured yet. Request persisted for bridge consumption."
            );
        }
        return new CodexExecutionDispatchResult(
            request.executionRequestId(),
            "",
            "dispatched",
            "Codex App Server bridge integration is configured but thread/start transport is not implemented yet."
        );
    }
}
