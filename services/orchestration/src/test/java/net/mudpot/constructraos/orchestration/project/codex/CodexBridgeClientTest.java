package net.mudpot.constructraos.orchestration.project.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.orchestration.config.CodexBridgeConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexBridgeClientTest {
    @Test
    void dispatchReturnsPlaceholderWhenBridgeDisabled() throws Exception {
        final CodexBridgeClient client = new CodexBridgeClient(disabledConfig(), new ObjectMapper());

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
                "Prepare the branch environment.",
                ""
            )
        );

        assertEquals("T-0001-exec-1", result.executionRequestId());
        assertEquals("dispatched", result.status());
    }

    private static CodexBridgeConfig disabledConfig() throws Exception {
        final CodexBridgeConfig config = new CodexBridgeConfig();
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
