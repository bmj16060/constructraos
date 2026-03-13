package net.mudpot.constructraos.codexbridge.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.codex.CodexConversationClient;

@Controller("/internal/codex")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CodexDispatchController {
    private final CodexConversationClient codexConversationClient;

    public CodexDispatchController(final CodexConversationClient codexConversationClient) {
        this.codexConversationClient = codexConversationClient;
    }

    @Post("/dispatch")
    public HttpResponse<CodexExecutionDispatchResult> dispatch(@Body final CodexExecutionDispatchRequest request) {
        return HttpResponse.ok(codexConversationClient.dispatch(request));
    }
}
