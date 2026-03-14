package net.mudpot.constructraos.codexruntime.model;

public record CodexRuntimeHealthResponse(
    String status,
    boolean configured,
    String message
) {
}
