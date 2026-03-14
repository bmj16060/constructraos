package net.mudpot.constructraos.orchestration.system.codex.execution;

import net.mudpot.constructraos.commons.orchestration.codex.execution.CodexCliJsonOutputParser;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CodexCliJsonOutputParserTest {
    private final CodexCliJsonOutputParser parser = new CodexCliJsonOutputParser();

    @Test
    void parsesStructuredAgentMessage() {
        final CodexCliJsonOutputParser.ParsedCodexCliOutput parsed = parser.parse(List.of(
            "{\"type\":\"thread.started\",\"thread_id\":\"thread-123\"}",
            "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"{\\\"status\\\":\\\"completed\\\",\\\"summary\\\":\\\"Structured response available.\\\",\\\"recommended_next_agent\\\":\\\"none\\\"}\"}}",
            "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":10,\"output_tokens\":5}}"
        ));

        assertEquals("thread-123", parsed.threadId());
        assertEquals(new CodexExecutionResult("completed", "Structured response available.", "none"), parsed.result());
        assertEquals("", parsed.errorMessage());
    }

    @Test
    void surfacesTurnFailure() {
        final CodexCliJsonOutputParser.ParsedCodexCliOutput parsed = parser.parse(List.of(
            "{\"type\":\"thread.started\",\"thread_id\":\"thread-123\"}",
            "{\"type\":\"turn.failed\",\"error\":{\"message\":\"Unauthorized\"}}"
        ));

        assertEquals("thread-123", parsed.threadId());
        assertNull(parsed.result());
        assertEquals("Unauthorized", parsed.errorMessage());
    }
}
