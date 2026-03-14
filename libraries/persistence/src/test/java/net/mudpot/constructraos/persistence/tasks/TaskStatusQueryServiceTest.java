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

class TaskStatusQueryServiceTest {
    @Test
    void findByWorkflowIdCombinesTaskProjectStepResultAndTranscriptState() {
        final UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID taskId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        final UUID stepId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        final UUID agentSessionId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        final UUID transcriptId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        final ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setName("ConstructraOS");
        project.setRootPath("/workspace/ConstructraOS");

        final TaskEntity task = new TaskEntity();
        task.setId(taskId);
        task.setProjectId(projectId);
        task.setWorkflowId("wf-123");
        task.setGoal("Summarize the next step.");
        task.setStatus("completed");
        task.setRequestedAgentName("planner");
        task.setRequestedByKind("anonymous");
        task.setRequestedBySessionId("anon-session-1");
        task.setCreatedAt(Instant.parse("2026-03-14T01:00:00Z"));
        task.setCompletedAt(Instant.parse("2026-03-14T01:02:00Z"));

        final TaskStepEntity step = new TaskStepEntity();
        step.setId(stepId);
        step.setTaskId(taskId);
        step.setStepNumber(1);
        step.setAgentName("planner");
        step.setAgentSessionId(agentSessionId);
        step.setStatus("completed");

        final TaskStepResultEntity result = new TaskStepResultEntity();
        result.setTaskId(taskId);
        result.setTaskStepId(stepId);
        result.setStatus("completed");
        result.setSummary("The persistence slice is ready.");
        result.setRecommendedNextAgent("reviewer");
        result.setResultPayload(Map.of("status", "completed"));

        final AgentSessionEntity agentSession = new AgentSessionEntity();
        agentSession.setId(agentSessionId);
        agentSession.setTaskId(taskId);
        agentSession.setAgentName("planner");
        agentSession.setProviderSessionId("thread-123");

        final TranscriptRecordEntity transcript = new TranscriptRecordEntity();
        transcript.setId(transcriptId);
        transcript.setTaskId(taskId);
        transcript.setTaskStepId(stepId);
        transcript.setAgentSessionId(agentSessionId);
        transcript.setTranscriptKind("codex-cli-jsonl");
        transcript.setProviderSessionId("thread-123");
        transcript.setTranscriptPayload(List.of("{\"type\":\"thread.started\"}"));

        final TaskRepository taskRepository = repositoryProxy(
            TaskRepository.class,
            Map.of("findByWorkflowId", task, "findById", task)
        );
        final ProjectRepository projectRepository = repositoryProxy(
            ProjectRepository.class,
            Map.of("findById", project)
        );
        final TaskStepRepository taskStepRepository = repositoryProxy(
            TaskStepRepository.class,
            Map.of("findLatestByTaskId", step)
        );
        final TaskStepResultRepository taskStepResultRepository = repositoryProxy(
            TaskStepResultRepository.class,
            Map.of("findByTaskStepId", result)
        );
        final AgentSessionRepository agentSessionRepository = repositoryProxy(
            AgentSessionRepository.class,
            Map.of("findById", agentSession)
        );
        final TranscriptRecordRepository transcriptRecordRepository = repositoryProxy(
            TranscriptRecordRepository.class,
            Map.of("findByTaskStepId", transcript)
        );

        final TaskStatusQueryService service = new TaskStatusQueryService(
            taskRepository,
            projectRepository,
            taskStepRepository,
            taskStepResultRepository,
            agentSessionRepository,
            transcriptRecordRepository
        );

        final Optional<TaskStatusView> resultView = service.findByWorkflowId("wf-123");

        assertTrue(resultView.isPresent());
        assertEquals("ConstructraOS", resultView.get().projectName());
        assertEquals("completed", resultView.get().latestStepStatus());
        assertEquals("reviewer", resultView.get().recommendedNextAgent());
        assertEquals("thread-123", resultView.get().providerSessionId());
        assertEquals(transcriptId, resultView.get().transcriptRecordId());
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
