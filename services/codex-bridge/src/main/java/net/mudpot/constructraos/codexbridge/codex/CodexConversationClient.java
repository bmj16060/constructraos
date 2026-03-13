package net.mudpot.constructraos.codexbridge.codex;

import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.CodexExecutionDispatchResult;

public interface CodexConversationClient {
    CodexExecutionDispatchResult dispatch(CodexExecutionDispatchRequest request);
}
