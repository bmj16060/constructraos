package net.mudpot.constructraos.persistence.history;

import net.mudpot.constructraos.commons.orchestration.system.model.HelloHistoryEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloHistoryQueryServiceTest {
    @Test
    void recentMapsPromptRunsIntoSharedHistoryEntries() {
        final PromptRunEntity entity = new PromptRunEntity();
        entity.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        entity.setWorkflowId("wf-1");
        entity.setUserName("Brandon");
        entity.setUseCase("Club operations");
        entity.setResponseText("Hello there.");
        entity.setProvider("openai-compatible");
        entity.setModel("demo");
        entity.setCacheHit(true);
        entity.setPromptTemplate("starter_hello_v1");
        entity.setCreatedAt(Instant.parse("2026-03-12T00:00:00Z"));

        final PromptRunRepository repository = (PromptRunRepository) Proxy.newProxyInstance(
            PromptRunRepository.class.getClassLoader(),
            new Class<?>[] {PromptRunRepository.class},
            (proxy, method, args) -> {
                if (method.getName().equals("findRecent")) {
                    return List.of(entity);
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );

        final HelloHistoryQueryService service = new HelloHistoryQueryService(repository);

        final List<HelloHistoryEntry> result = service.recent(12);

        assertEquals(1, result.size());
        assertEquals("wf-1", result.getFirst().workflowId());
        assertEquals("Brandon", result.getFirst().name());
        assertEquals("starter_hello_v1", result.getFirst().promptTemplate());
    }
}
