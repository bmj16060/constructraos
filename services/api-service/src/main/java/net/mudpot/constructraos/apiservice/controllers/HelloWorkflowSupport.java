package net.mudpot.constructraos.apiservice.controllers;

import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldRequest;

import java.util.Map;

public final class HelloWorkflowSupport {
    private HelloWorkflowSupport() {
    }

    public static HelloWorldRequest normalizedRequest(final HelloWorldRequest request) {
        if (request == null) {
            return new HelloWorldRequest("World", "Demonstrate the ConstructraOS platform baseline.", "");
        }
        final String name = request.name() == null || request.name().isBlank() ? "World" : request.name().trim();
        final String useCase = request.useCase() == null || request.useCase().isBlank()
            ? "Demonstrate the ConstructraOS platform baseline."
            : request.useCase().trim();
        final String workflowId = request.workflowId() == null ? "" : request.workflowId().trim();
        return new HelloWorldRequest(name, useCase, workflowId);
    }

    public static int normalizedLimit(final int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    public static Map<String, Object> actorInput(final AnonymousSession session) {
        return Map.of(
            "kind", session.actorKind(),
            "session_id", session.sessionId()
        );
    }
}
