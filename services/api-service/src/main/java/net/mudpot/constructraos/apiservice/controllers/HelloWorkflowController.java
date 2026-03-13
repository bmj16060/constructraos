package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloHistoryEntry;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldRequest;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.HelloWorldWorkflowClient;
import net.mudpot.constructraos.persistence.history.HelloHistoryQueryService;

import java.util.List;
import java.util.Map;

@Controller("/api/workflows/hello-world")
@ExecuteOn(TaskExecutors.BLOCKING)
public class HelloWorkflowController {
    private final HelloWorldWorkflowClient helloWorldWorkflowClient;
    private final HelloHistoryQueryService helloHistoryQueryService;
    private final AnonymousSessionService anonymousSessionService;
    private final PolicyEvaluator policyEvaluator;

    public HelloWorkflowController(
        final HelloWorldWorkflowClient helloWorldWorkflowClient,
        final HelloHistoryQueryService helloHistoryQueryService,
        final AnonymousSessionService anonymousSessionService,
        final PolicyEvaluator policyEvaluator
    ) {
        this.helloWorldWorkflowClient = helloWorldWorkflowClient;
        this.helloHistoryQueryService = helloHistoryQueryService;
        this.anonymousSessionService = anonymousSessionService;
        this.policyEvaluator = policyEvaluator;
    }

    @Post("/run")
    public MutableHttpResponse<HelloWorldResult> run(final HttpRequest<?> httpRequest, @Body final HelloWorldRequest request) {
        final HelloWorldRequest normalized = normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requirePolicy("workflow.hello_world.run", normalized, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloWorldWorkflowClient.run(normalized.name(), normalized.useCase(), session.actorKind(), session.sessionId())),
            session
        );
    }

    @Post("/start")
    public MutableHttpResponse<WorkflowStartResponse> start(final HttpRequest<?> httpRequest, @Body final HelloWorldRequest request) {
        final HelloWorldRequest normalized = normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requirePolicy("workflow.hello_world.start", normalized, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloWorldWorkflowClient.start(normalized.name(), normalized.useCase(), normalized.workflowId(), session.actorKind(), session.sessionId())),
            session
        );
    }

    @Get("/history")
    public MutableHttpResponse<List<HelloHistoryEntry>> history(final HttpRequest<?> httpRequest, @QueryValue(defaultValue = "12") final int limit) {
        final int resolvedLimit = Math.max(1, Math.min(limit, 50));
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requirePolicy("workflow.hello_world.history", null, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloHistoryQueryService.recent(resolvedLimit)),
            session
        );
    }

    private void requirePolicy(final String action, final HelloWorldRequest request, final AnonymousSession session) {
        final Map<String, Object> actor = Map.of(
            "kind", session.actorKind(),
            "session_id", session.sessionId()
        );
        final Map<String, Object> input = request == null
            ? Map.of(
                "actor", actor,
                "resource", Map.of("type", "hello-world", "scope", "history")
            )
            : Map.of(
                "actor", actor,
                "resource", Map.of("type", "hello-world", "scope", "workflow"),
                "name", request.name(),
                "use_case", request.useCase()
            );
        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Policy denied: " + result.reason());
        }
    }

    private static HelloWorldRequest normalizedRequest(final HelloWorldRequest request) {
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
}
