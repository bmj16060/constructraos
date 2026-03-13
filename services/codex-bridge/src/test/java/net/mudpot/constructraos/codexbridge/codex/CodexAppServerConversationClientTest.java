package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.config.CodexAppServerConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexAppServerConversationClientTest {
    @Test
    void dispatchReturnsPlaceholderWhenAppServerDisabled() throws Exception {
        final CodexAppServerConversationClient client = new CodexAppServerConversationClient(disabledConfig(), new ObjectMapper());

        final CodexExecutionDispatchResult result = client.dispatch(
            new CodexExecutionDispatchRequest(
                "constructraos",
                "T-0001",
                "T-0001-exec-1",
                "SRE",
                "project/constructraos/integration",
                "runtime/workspaces",
                "project-constructraos-task-t-0001",
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                "anonymous",
                "anon-session-1",
                "Prepare the branch environment."
            )
        );

        assertEquals("T-0001-exec-1", result.executionRequestId());
        assertEquals("dispatched", result.status());
    }

    private static CodexAppServerConfig disabledConfig() throws Exception {
        final CodexAppServerConfig config = new CodexAppServerConfig();
        set(config, "enabled", false);
        set(config, "url", "");
        set(config, "timeoutSeconds", 10);
        return config;
    }

    private static void set(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
