package net.mudpot.constructraos.persistence.tasks;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTranscriptQueryServiceTest {
    @Test
    void findLatestByWorkflowIdReturnsLatestTranscriptForWorkflow() {
        final UUID taskId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        final UUID stepId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        final UUID transcriptId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        final TaskEntity task = new TaskEntity();
        task.setId(taskId);
        task.setWorkflowId("wf-123");

        final TaskStepEntity step = new TaskStepEntity();
        step.setId(stepId);
        step.setTaskId(taskId);
        step.setStepNumber(1);
        step.setAgentName("planner");
        step.setStatus("completed");

        final TranscriptRecordEntity transcript = new TranscriptRecordEntity();
        transcript.setId(transcriptId);
        transcript.setTaskId(taskId);
        transcript.setTaskStepId(stepId);
        transcript.setTranscriptKind("codex-cli-jsonl");
        transcript.setProviderSessionId("thread-123");
        transcript.setTranscriptPayload(List.of("{\"type\":\"thread.started\"}"));
        transcript.setCreatedAt(Instant.parse("2026-03-14T01:00:00Z"));
        transcript.setUpdatedAt(Instant.parse("2026-03-14T01:01:00Z"));

        final TaskRepository taskRepository = repositoryProxy(TaskRepository.class, Map.of("findByWorkflowId", task));
        final TaskStepRepository taskStepRepository = repositoryProxy(TaskStepRepository.class, Map.of("findLatestByTaskId", step));
        final TranscriptRecordRepository transcriptRecordRepository = repositoryProxy(TranscriptRecordRepository.class, Map.of("findByTaskStepId", transcript));

        final TaskTranscriptQueryService service = new TaskTranscriptQueryService(
            taskRepository,
            taskStepRepository,
            transcriptRecordRepository
        );

        final Optional<TaskTranscriptView> result = service.findLatestByWorkflowId("wf-123");

        assertTrue(result.isPresent());
        assertEquals("wf-123", result.get().workflowId());
        assertEquals("planner", result.get().latestStepAgentName());
        assertEquals(transcriptId, result.get().transcriptRecordId());
        assertEquals("thread-123", result.get().providerSessionId());
    }

    @SuppressWarnings("unchecked")
    private static <T> T repositoryProxy(final Class<T> repositoryType, final Map<String, Object> returnsByMethod) {
        return (T) Proxy.newProxyInstance(
            repositoryType.getClassLoader(),
            new Class<?>[] {repositoryType},
            (proxy, method, args) -> {
                if (method.getName().equals("toString")) {
                    return repositoryType.getSimpleName() + "Proxy";
                }
                if (returnsByMethod.containsKey(method.getName())) {
                    final Object value = returnsByMethod.get(method.getName());
                    return Optional.class.isAssignableFrom(method.getReturnType()) ? Optional.ofNullable(value) : value;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }
}
