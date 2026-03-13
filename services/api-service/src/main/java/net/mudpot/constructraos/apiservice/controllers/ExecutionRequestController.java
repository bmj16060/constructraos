package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;

import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.FORBIDDEN;

@Controller("/api/projects")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ExecutionRequestController {
    private final ProjectRecordsGateway projectRecordsGateway;
    private final AnonymousSessionService anonymousSessionService;
    private final PolicyEvaluator policyEvaluator;

    public ExecutionRequestController(
        final ProjectRecordsGateway projectRecordsGateway,
        final AnonymousSessionService anonymousSessionService,
        final PolicyEvaluator policyEvaluator
    ) {
        this.projectRecordsGateway = projectRecordsGateway;
        this.anonymousSessionService = anonymousSessionService;
        this.policyEvaluator = policyEvaluator;
    }

    @Get("/{projectId}/execution-requests")
    public MutableHttpResponse<List<ProjectExecutionRequestRecord>> listExecutionRequests(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @QueryValue(defaultValue = "") final String status
    ) {
        final String normalizedProjectId = normalize(projectId);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.execution_request.list", normalizedProjectId, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(projectRecordsGateway.listExecutionRequests(normalizedProjectId, status)),
            session
        );
    }

    private void requireAuth(final String action, final String projectId, final AnonymousSession session) {
        final Map<String, Object> actor = Map.of(
            "kind", session.actorKind(),
            "session_id", session.sessionId()
        );
        final Map<String, Object> input = Map.of(
            "actor", actor,
            "resource", Map.of(
                "type", "project-execution-request",
                "project_id", projectId
            )
        );
        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new io.micronaut.http.exceptions.HttpStatusException(FORBIDDEN, "Policy denied: " + result.reason());
        }
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
