package net.mudpot.constructraos.commons.orchestration.codex.execution;

import java.util.List;

public class CodexExecutionException extends RuntimeException {
    private final String sessionId;
    private final List<String> transcriptLines;

    public CodexExecutionException(final String message, final String sessionId, final List<String> transcriptLines) {
        super(message);
        this.sessionId = sessionId == null ? "" : sessionId.trim();
        this.transcriptLines = transcriptLines == null ? List.of() : List.copyOf(transcriptLines);
    }

    public CodexExecutionException(final String message, final Throwable cause, final String sessionId, final List<String> transcriptLines) {
        super(message, cause);
        this.sessionId = sessionId == null ? "" : sessionId.trim();
        this.transcriptLines = transcriptLines == null ? List.of() : List.copyOf(transcriptLines);
    }

    public String sessionId() {
        return sessionId;
    }

    public List<String> transcriptLines() {
        return transcriptLines;
    }
}
