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
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.policy.AuthPolicy;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloHistoryEntry;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldRequest;
import net.mudpot.constructraos.commons.orchestration.system.model.HelloWorldResult;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.HelloWorldWorkflowClient;
import net.mudpot.constructraos.persistence.history.HelloHistoryQueryService;

import java.util.List;

@Controller("/api/workflows/hello-world")
@ExecuteOn(TaskExecutors.BLOCKING)
public class HelloWorkflowController {
    private final HelloWorldWorkflowClient helloWorldWorkflowClient;
    private final HelloHistoryQueryService helloHistoryQueryService;
    private final AnonymousSessionService anonymousSessionService;

    public HelloWorkflowController(
        final HelloWorldWorkflowClient helloWorldWorkflowClient,
        final HelloHistoryQueryService helloHistoryQueryService,
        final AnonymousSessionService anonymousSessionService
    ) {
        this.helloWorldWorkflowClient = helloWorldWorkflowClient;
        this.helloHistoryQueryService = helloHistoryQueryService;
        this.anonymousSessionService = anonymousSessionService;
    }

    @AuthPolicy("api.hello_world.run")
    @Post("/run")
    public MutableHttpResponse<HelloWorldResult> run(final HttpRequest<?> httpRequest, @Body final HelloWorldRequest request) {
        final HelloWorldRequest normalized = HelloWorkflowSupport.normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloWorldWorkflowClient.run(normalized.name(), normalized.useCase(), session.actorKind(), session.sessionId())),
            session
        );
    }

    @AuthPolicy("api.hello_world.start")
    @Post("/start")
    public MutableHttpResponse<WorkflowStartResponse> start(final HttpRequest<?> httpRequest, @Body final HelloWorldRequest request) {
        final HelloWorldRequest normalized = HelloWorkflowSupport.normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloWorldWorkflowClient.start(normalized.name(), normalized.useCase(), normalized.workflowId(), session.actorKind(), session.sessionId())),
            session
        );
    }

    @AuthPolicy("api.hello_world.read_history")
    @Get("/history")
    public MutableHttpResponse<List<HelloHistoryEntry>> history(final HttpRequest<?> httpRequest, @QueryValue(defaultValue = "12") final int limit) {
        final int resolvedLimit = HelloWorkflowSupport.normalizedLimit(limit);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(helloHistoryQueryService.recent(resolvedLimit)),
            session
        );
    }
}
