package net.mudpot.constructraos.codexruntime.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionRequest;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionResponse;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeHealthResponse;
import net.mudpot.constructraos.codexruntime.service.CodexRuntimeOperations;

@Controller
public class CodexRuntimeController {
    private final CodexRuntimeOperations operations;

    public CodexRuntimeController(final CodexRuntimeOperations operations) {
        this.operations = operations;
    }

    @Get("/healthz")
    public HttpResponse<CodexRuntimeHealthResponse> health() {
        final CodexRuntimeHealthResponse response = operations.health();
        return response.configured() ? HttpResponse.ok(response) : HttpResponse.serverError(response);
    }

    @Post("/executions")
    public HttpResponse<CodexRuntimeExecutionResponse> execute(@Body final CodexRuntimeExecutionRequest request) {
        final CodexRuntimeOperations.CodexRuntimeExecutionOutcome outcome = operations.execute(request);
        final HttpStatus status = switch (outcome.statusCode()) {
            case 200 -> HttpStatus.OK;
            case 400 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return HttpResponse.status(status).body(outcome.response());
    }
}
