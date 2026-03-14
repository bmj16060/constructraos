package net.mudpot.constructraos.apiservice.tasks;

public record TaskActorContext(
    String actorKind,
    String sessionId
) {
    public static TaskActorContext mcp() {
        return new TaskActorContext("mcp", "mcp-tool");
    }
}
