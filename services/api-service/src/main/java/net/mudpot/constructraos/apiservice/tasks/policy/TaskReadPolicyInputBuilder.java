package net.mudpot.constructraos.apiservice.tasks.policy;

import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.policy.PolicyInputBuilder;
import net.mudpot.constructraos.apiservice.tasks.TaskActorContext;
import net.mudpot.constructraos.apiservice.tasks.TaskSurfaceNormalization;

import java.util.Map;

@Singleton
public class TaskReadPolicyInputBuilder implements PolicyInputBuilder {
    @Override
    public Map<String, Object> build(final MethodInvocationContext<Object, Object> context) {
        final String workflowId = TaskSurfaceNormalization.normalizedWorkflowId((String) context.getParameterValues()[0]);
        final TaskActorContext actor = (TaskActorContext) context.getParameterValues()[1];
        return Map.of(
            "actor", TaskSurfaceNormalization.actorInput(actor),
            "resource", Map.of("type", "task", "scope", "status"),
            "workflow_id", workflowId
        );
    }
}
