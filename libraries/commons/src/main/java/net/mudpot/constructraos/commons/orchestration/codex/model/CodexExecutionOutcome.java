package net.mudpot.constructraos.commons.orchestration.codex.model;

import java.util.List;

public record CodexExecutionOutcome(
    CodexExecutionResult result,
    String sessionId,
    List<String> transcriptLines
) {
    public CodexExecutionOutcome {
        if (result == null) {
            throw new IllegalArgumentException("Codex execution result is required.");
        }
        sessionId = sessionId == null ? "" : sessionId.trim();
        transcriptLines = transcriptLines == null ? List.of() : List.copyOf(transcriptLines);
    }
}
