package net.mudpot.constructraos.apiservice.session;

import java.time.Instant;

public record AnonymousSession(
    String sessionId,
    String actorKind,
    Instant issuedAt,
    boolean fresh
) {
}
