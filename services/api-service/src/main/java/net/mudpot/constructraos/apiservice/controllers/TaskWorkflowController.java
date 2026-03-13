package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.apiservice.workflow.TaskWorkflowOperationsService;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.util.Map;

import static io.micronaut.http.HttpStatus.FORBIDDEN;
import static io.micronaut.http.HttpStatus.NOT_FOUND;

@Controller("/api/projects")
@ExecuteOn(TaskExecutors.BLOCKING)
public class TaskWorkflowController {
    private final TaskWorkflowOperationsService taskWorkflowOperationsService;
    private final AnonymousSessionService anonymousSessionService;
    private final PolicyEvaluator policyEvaluator;

    public TaskWorkflowController(
        final TaskWorkflowOperationsService taskWorkflowOperationsService,
        final AnonymousSessionService anonymousSessionService,
        final PolicyEvaluator policyEvaluator
    ) {
        this.taskWorkflowOperationsService = taskWorkflowOperationsService;
        this.anonymousSessionService = anonymousSessionService;
        this.policyEvaluator = policyEvaluator;
    }

    @Post("/{projectId}/tasks/{taskId}/qa-requests")
    public MutableHttpResponse<TaskWorkflowSignalResponse> requestQa(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @PathVariable final String taskId,
        @Body final TaskQaRequestBody request
    ) {
        final String normalizedProjectId = normalize(projectId);
        final String normalizedTaskId = normalize(taskId);
        final TaskQaRequestBody normalizedRequest = request == null ? new TaskQaRequestBody("", "") : request;
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.task.qa_request", normalizedProjectId, normalizedTaskId, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                taskWorkflowOperationsService.requestQa(
                    normalizedProjectId,
                    normalizedTaskId,
                    normalizedRequest.branchName(),
                    normalizedRequest.note(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }

    @Get("/{projectId}/tasks/{taskId}/workflow")
    public MutableHttpResponse<TaskWorkflowState> currentState(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @PathVariable final String taskId
    ) {
        final String normalizedProjectId = normalize(projectId);
        final String normalizedTaskId = normalize(taskId);
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.task.workflow.view", normalizedProjectId, normalizedTaskId, session);
        try {
            return anonymousSessionService.attachCookieIfNeeded(
                HttpResponse.ok(taskWorkflowOperationsService.currentState(normalizedProjectId, normalizedTaskId)),
                session
            );
        } catch (RuntimeException exception) {
            throw new HttpStatusException(NOT_FOUND, "Task workflow not found.");
        }
    }

    @Post("/{projectId}/tasks/{taskId}/sre-environment-outcomes")
    public MutableHttpResponse<TaskWorkflowSignalResponse> reportSreEnvironmentOutcome(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @PathVariable final String taskId,
        @Body final TaskSreEnvironmentOutcomeBody request
    ) {
        final String normalizedProjectId = normalize(projectId);
        final String normalizedTaskId = normalize(taskId);
        final TaskSreEnvironmentOutcomeBody normalizedRequest = request == null
            ? new TaskSreEnvironmentOutcomeBody("", "", "", "")
            : request;
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.task.sre_environment.report", normalizedProjectId, normalizedTaskId, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                taskWorkflowOperationsService.reportSreEnvironmentOutcome(
                    normalizedProjectId,
                    normalizedTaskId,
                    normalizedRequest.branchName(),
                    normalizedRequest.environmentName(),
                    normalizedRequest.status(),
                    normalizedRequest.note(),
                    session.actorKind(),
                    session.sessionId()
                )
            ),
            session
        );
    }

    @Post("/{projectId}/tasks/{taskId}/codex-executions/accepted")
    public MutableHttpResponse<TaskWorkflowSignalResponse> reportCodexExecutionAccepted(
        final HttpRequest<?> httpRequest,
        @PathVariable final String projectId,
        @PathVariable final String taskId,
        @Body final CodexExecutionAcceptedBody request
    ) {
        final String normalizedProjectId = normalize(projectId);
        final String normalizedTaskId = normalize(taskId);
        final CodexExecutionAcceptedBody normalizedRequest = request == null
            ? new CodexExecutionAcceptedBody("", "", "", "")
            : request;
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        requireAuth("project.task.codex_execution.accepted", normalizedProjectId, normalizedTaskId, session);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(
                taskWorkflowOperationsService.reportCodexExecutionAccepted(
                    normalizedProjectId,
                    normalizedTaskId,
                    normalizedRequest.executionRequestId(),
                    normalizedRequest.codexThreadId(),
                    normalizedRequest.specialistRole(),
                    normalizedRequest.note()
                )
            ),
            session
        );
    }

    private void requireAuth(final String action, final String projectId, final String taskId, final AnonymousSession session) {
        final Map<String, Object> actor = Map.of(
            "kind", session.actorKind(),
            "session_id", session.sessionId()
        );
        final Map<String, Object> input = Map.of(
            "actor", actor,
            "resource", Map.of(
                "type", "project-task-workflow",
                "project_id", projectId,
                "task_id", taskId
            )
        );
        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(FORBIDDEN, "Policy denied: " + result.reason());
        }
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }

    record TaskQaRequestBody(String branchName, String note) {
    }

    record TaskSreEnvironmentOutcomeBody(String branchName, String environmentName, String status, String note) {
    }

    record CodexExecutionAcceptedBody(String executionRequestId, String codexThreadId, String specialistRole, String note) {
    }
}
