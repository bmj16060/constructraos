package net.mudpot.constructraos.apiservice.session;

public record AnonymousSessionResponse(
    String sessionId,
    String actorKind,
    String issuedAt
) {
}
