package net.mudpot.constructraos.commons.orchestration.codex.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

import java.util.List;

public final class CodexCliJsonOutputParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ParsedCodexCliOutput parse(final List<String> lines) {
        String threadId = "";
        String agentMessage = "";
        String failureMessage = "";

        for (final String rawLine : lines) {
            final String line = rawLine == null ? "" : rawLine.trim();
            if (!line.startsWith("{")) {
                continue;
            }
            try {
                final JsonNode node = OBJECT_MAPPER.readTree(line);
                final String type = node.path("type").asText("");
                if ("thread.started".equals(type)) {
                    threadId = node.path("thread_id").asText("");
                    continue;
                }
                if ("turn.failed".equals(type)) {
                    failureMessage = node.path("error").path("message").asText("Codex execution failed.");
                    continue;
                }
                if (!"item.completed".equals(type)) {
                    continue;
                }
                final JsonNode item = node.path("item");
                if ("agent_message".equals(item.path("type").asText(""))) {
                    agentMessage = item.path("text").asText("");
                }
            } catch (final Exception ignored) {
                // Non-JSON or non-event lines are intentionally ignored.
            }
        }

        if (!failureMessage.isBlank()) {
            return new ParsedCodexCliOutput(threadId, null, failureMessage);
        }
        if (agentMessage.isBlank()) {
            return new ParsedCodexCliOutput(threadId, null, "Codex execution completed without a structured response.");
        }

        try {
            return new ParsedCodexCliOutput(
                threadId,
                OBJECT_MAPPER.readValue(agentMessage, CodexExecutionResult.class),
                ""
            );
        } catch (final Exception exception) {
            return new ParsedCodexCliOutput(threadId, null, "Codex execution returned invalid structured output.");
        }
    }

    public record ParsedCodexCliOutput(
        String threadId,
        CodexExecutionResult result,
        String errorMessage
    ) {
    }
}
