package net.mudpot.constructraos.apiservice.tasks.policy;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.policy.PolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceNormalization;

import java.util.Map;

@Singleton
public class TaskListPolicyInputBuilder implements PolicyInputBuilder {
    private final String defaultWorkingDirectory;

    public TaskListPolicyInputBuilder(@Value("${task.default-working-directory:}") final String defaultWorkingDirectory) {
        this.defaultWorkingDirectory = defaultWorkingDirectory;
    }

    @Override
    public Map<String, Object> build(final MethodInvocationContext<Object, Object> context) {
        final String workingDirectory = TaskSurfaceNormalization.normalizedWorkingDirectory((String) context.getParameterValues()[0], defaultWorkingDirectory);
        final int limit = TaskSurfaceNormalization.normalizedLimit((Integer) context.getParameterValues()[1]);
        final TaskActorContext actor = (TaskActorContext) context.getParameterValues()[2];
        return Map.of(
            "actor", TaskSurfaceNormalization.actorInput(actor),
            "resource", Map.of("type", "task", "scope", "list"),
            "working_directory", workingDirectory,
            "limit", limit
        );
    }
}
