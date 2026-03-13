package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionClaimRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.projectrecords.ProjectRecordsGateway;
import net.mudpot.constructraos.clients.project.TaskCoordinationWorkflowClient;

import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.FORBIDDEN;
import static io.micronaut.http.HttpStatus.CONFLICT;

@Controller("/api/projects")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ExecutionRequestController {
    private final ProjectRecordsGateway projectRecordsGateway;
    private final TaskCoordinationWorkflowClient taskCoordinationWorkflowClient;
    private final AnonymousSessionService anonymousSessionService;
    private final PolicyEvaluator policyEvaluator;

    public ExecutionRequestController(
        final ProjectRecordsGateway projectRecordsGateway,
        final TaskCoordinationWorkflowClient taskCoordinationWorkflowClient,
        final AnonymousSessionService anonymousSessionService,
        final PolicyEvaluator policyEvaluator
    ) {
        this.projectRecordsGateway = projectRecordsGateway;
        this.taskCoordinationWorkflowClient = taskCoordinationWorkflowClient;
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

    @Post("/{projectId}/execution-requests/{executionRequestId}/claim")
    public MutableHttpResponse<ProjectExecutionRequestRecord> claimExecutionRequest(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @PathVariable final String executionRequestId,
        @Body final ClaimExecutionRequestBody request
    ) {
        final String normalizedProjectId = normalize(projectId);
        final String normalizedExecutionRequestId = normalize(executionRequestId);
        final ClaimExecutionRequestBody normalizedRequest = request == null
            ? new ClaimExecutionRequestBody("", "", "")
            : request;
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.execution_request.claim", normalizedProjectId, session);
        try {
            final ProjectExecutionRequestRecord claimed = projectRecordsGateway.claimExecutionRequest(
                new ProjectExecutionClaimRequest(
                    normalizedProjectId,
                    normalizedExecutionRequestId,
                    normalizedRequest.codexThreadId(),
                    normalizedRequest.note()
                )
            );
            taskCoordinationWorkflowClient.reportCodexExecutionAccepted(
                normalizedProjectId,
                claimed.taskId(),
                claimed.id(),
                claimed.codexThreadId(),
                claimed.specialistRole(),
                normalizedRequest.note()
            );
            return anonymousSessionService.attachCookieIfNeeded(HttpResponse.ok(claimed), session);
        } catch (IllegalStateException exception) {
            throw new io.micronaut.http.exceptions.HttpStatusException(CONFLICT, exception.getMessage());
        }
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

    record ClaimExecutionRequestBody(String codexThreadId, String specialistRole, String note) {
    }
}
