package net.mudpot.constructraos.codexruntime.controller;

import io.micronaut.http.HttpResponse;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionRequest;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionResponse;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeHealthResponse;
import net.mudpot.constructraos.codexruntime.service.CodexRuntimeOperations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexRuntimeControllerTest {
    @Test
    void healthReturnsServerErrorWhenRuntimeIsUnconfigured() {
        final CodexRuntimeController controller = new CodexRuntimeController(new StubOperations(
            new CodexRuntimeHealthResponse("unconfigured", false, "missing OPENAI_API_KEY"),
            new CodexRuntimeOperations.CodexRuntimeExecutionOutcome(500, new CodexRuntimeExecutionResponse(-1, List.of(), "unused"))
        ));

        final HttpResponse<CodexRuntimeHealthResponse> response = controller.health();

        assertEquals(500, response.code());
        assertEquals("unconfigured", response.body().status());
    }

    @Test
    void executeReturnsConfiguredOutcomeStatus() {
        final CodexRuntimeController controller = new CodexRuntimeController(new StubOperations(
            new CodexRuntimeHealthResponse("ok", true, "configured"),
            new CodexRuntimeOperations.CodexRuntimeExecutionOutcome(
                200,
                new CodexRuntimeExecutionResponse(0, List.of("{\"type\":\"item.completed\"}"), "")
            )
        ));

        final HttpResponse<CodexRuntimeExecutionResponse> response = controller.execute(
            new CodexRuntimeExecutionRequest("Prompt", "", "{}", 10L)
        );

        assertEquals(200, response.code());
        assertEquals(0, response.body().exitCode());
    }

    private record StubOperations(
        CodexRuntimeHealthResponse health,
        CodexRuntimeOperations.CodexRuntimeExecutionOutcome execution
    ) implements CodexRuntimeOperations {
        @Override
        public CodexRuntimeHealthResponse health() {
            return health;
        }

        @Override
        public CodexRuntimeExecutionOutcome execute(final CodexRuntimeExecutionRequest request) {
            return execution;
        }
    }
}
