package net.mudpot.constructraos.codexbridge.controllers;

import io.micronaut.http.HttpResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;
import net.mudpot.constructraos.codexbridge.codex.CodexConversationClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexDispatchControllerTest {
    @Test
    void dispatchReturnsConversationClientResult() {
        final CodexDispatchController controller = new CodexDispatchController(
            request -> new CodexExecutionDispatchResult(request.executionRequestId(), "", "dispatched", "Bridge accepted request.")
        );

        final HttpResponse<CodexExecutionDispatchResult> response = controller.dispatch(
            new CodexExecutionDispatchRequest(
                "constructraos",
                "T-0001",
                "T-0001-exec-1",
                "SRE",
                "project/constructraos/integration",
                "runtime/workspaces",
                "project-constructraos-task-t-0001",
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                "anonymous",
                "anon-session-1",
                "Prepare the branch environment.",
                ""
            )
        );

        assertEquals("dispatched", response.body().status());
    }
}
