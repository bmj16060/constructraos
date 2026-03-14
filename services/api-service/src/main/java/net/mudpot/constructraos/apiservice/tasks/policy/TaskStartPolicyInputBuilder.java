package net.mudpot.constructraos.apiservice.tasks.policy;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.policy.PolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskStartRequest;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceNormalization;

import java.util.Map;

@Singleton
public class TaskStartPolicyInputBuilder implements PolicyInputBuilder {
    private final String defaultWorkingDirectory;

    public TaskStartPolicyInputBuilder(@Value("${task.default-working-directory:}") final String defaultWorkingDirectory) {
        this.defaultWorkingDirectory = defaultWorkingDirectory;
    }

    @Override
    public Map<String, Object> build(final MethodInvocationContext<Object, Object> context) {
        final TaskStartRequest request = (TaskStartRequest) context.getParameterValues()[0];
        final TaskActorContext actor = (TaskActorContext) context.getParameterValues()[1];
        final TaskStartRequest normalized = TaskSurfaceNormalization.normalizedRequest(request, defaultWorkingDirectory);
        return Map.of(
            "actor", TaskSurfaceNormalization.actorInput(actor),
            "resource", Map.of("type", "task", "scope", "start"),
            "goal", normalized.goal(),
            "working_directory", normalized.workingDirectory(),
            "agent_name", normalized.agentName()
        );
    }
}
