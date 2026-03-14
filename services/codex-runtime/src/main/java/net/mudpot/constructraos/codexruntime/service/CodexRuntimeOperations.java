package net.mudpot.constructraos.codexruntime.service;

import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionRequest;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeExecutionResponse;
import net.mudpot.constructraos.codexruntime.model.CodexRuntimeHealthResponse;

public interface CodexRuntimeOperations {
    CodexRuntimeHealthResponse health();

    CodexRuntimeExecutionOutcome execute(CodexRuntimeExecutionRequest request);

    record CodexRuntimeExecutionOutcome(int statusCode, CodexRuntimeExecutionResponse response) {
    }
}
