package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.apiservice.policy.AuthPolicy;
import net.mudpot.constructraos.apiservice.session.AnonymousSession;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskStartResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskStatusResponse;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceService;

import java.util.List;

@Controller("/api/tasks")
@ExecuteOn(TaskExecutors.BLOCKING)
public class TaskController {
    private final TaskSurfaceService taskSurfaceService;
    private final AnonymousSessionService anonymousSessionService;

    public TaskController(
        final TaskSurfaceService taskSurfaceService,
        final AnonymousSessionService anonymousSessionService
    ) {
        this.taskSurfaceService = taskSurfaceService;
        this.anonymousSessionService = anonymousSessionService;
    }

    @AuthPolicy("api.tasks.start")
    @Post("/start")
    public MutableHttpResponse<TaskStartResponse> start(final HttpRequest<?> httpRequest, @Body final TaskStartRequest request) {
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(taskSurfaceService.startTask(request, actorContext(session))),
            session
        );
    }

    @AuthPolicy("api.tasks.read")
    @Get("/{workflowId}")
    public MutableHttpResponse<TaskStatusResponse> status(final HttpRequest<?> httpRequest, @PathVariable final String workflowId) {
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(taskSurfaceService.getTaskStatus(workflowId, actorContext(session))),
            session
        );
    }

    @AuthPolicy("api.tasks.read")
    @Get
    public MutableHttpResponse<List<TaskStatusResponse>> recent(
        final HttpRequest<?> httpRequest,
        @QueryValue(value = "working_directory", defaultValue = "") final String workingDirectory,
        @QueryValue(defaultValue = "12") final int limit
    ) {
        final AnonymousSession session = anonymousSessionService.ensureSession(httpRequest);
        return anonymousSessionService.attachCookieIfNeeded(
            HttpResponse.ok(taskSurfaceService.listTasks(workingDirectory, limit, actorContext(session))),
            session
        );
    }

    private static TaskActorContext actorContext(final AnonymousSession session) {
        return new TaskActorContext(session.actorKind(), session.sessionId());
    }
}
