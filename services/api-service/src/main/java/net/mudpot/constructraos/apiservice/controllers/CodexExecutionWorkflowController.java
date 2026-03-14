package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.policy.AuthPolicy;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.clients.model.WorkflowStartResponse;
import net.mudpot.constructraos.clients.system.CodexExecutionWorkflowClient;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionRequest;
import net.mudpot.constructraos.commons.orchestration.codex.model.CodexExecutionResult;

@Controller("/api/workflows/codex-execution")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CodexExecutionWorkflowController {
    private final CodexExecutionWorkflowClient codexExecutionWorkflowClient;
    private final AnonymousSessionService anonymousSessionService;

    public CodexExecutionWorkflowController(
        final CodexExecutionWorkflowClient codexExecutionWorkflowClient,
        final AnonymousSessionService anonymousSessionService
    ) {
        this.codexExecutionWorkflowClient = codexExecutionWorkflowClient;
        this.anonymousSessionService = anonymousSessionService;
    }

    @AuthPolicy("api.codex_execution.run")
    @Post("/run")
    public MutableHttpResponse<CodexExecutionResult> run(final HttpRequest<?> httpRequest, @Body final CodexExecutionRequest request) {
        final CodexExecutionRequest normalized = CodexExecutionWorkflowSupport.normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                codexExecutionWorkflowClient.run(
                    normalized.prompt(),
                    normalized.workingDirectory(),
                    normalized.agentName(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }

    @AuthPolicy("api.codex_execution.start")
    @Post("/start")
    public MutableHttpResponse<WorkflowStartResponse> start(final HttpRequest<?> httpRequest, @Body final CodexExecutionRequest request) {
        final CodexExecutionRequest normalized = CodexExecutionWorkflowSupport.normalizedRequest(request);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                codexExecutionWorkflowClient.start(
                    normalized.prompt(),
                    normalized.workingDirectory(),
                    normalized.agentName(),
                    normalized.workflowId(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }
}
