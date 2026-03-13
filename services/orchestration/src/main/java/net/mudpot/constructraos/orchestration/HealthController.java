package net.mudpot.constructraos.orchestration;

import net.mudpot.constructraos.orchestration.worker.TemporalWorkerRuntime;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Map;

@Controller
public class HealthController {

    private final TemporalWorkerRuntime runtime;

    public HealthController(final TemporalWorkerRuntime runtime) {
        this.runtime = runtime;
    }

    @Get("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }

    @Get("/readyz")
    public HttpResponse<Map<String, String>> readyz() {
        if (runtime.isReady()) {
            return HttpResponse.ok(Map.of("status", "ready"));
        }
        return HttpResponse.serverError(Map.of("status", "not-ready"));
    }
}
