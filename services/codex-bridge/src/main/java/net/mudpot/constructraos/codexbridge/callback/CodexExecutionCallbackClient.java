package net.mudpot.constructraos.codexbridge.callback;

import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;

public interface CodexExecutionCallbackClient {
    void reportAccepted(CodexExecutionDispatchRequest request, String codexThreadId, String note);
}
