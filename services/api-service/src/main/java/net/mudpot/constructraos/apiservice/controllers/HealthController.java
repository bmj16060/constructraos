package net.mudpot.constructraos.apiservice.controllers;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Map;

@Controller("/api")
public class HealthController {
    @Get("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @Get("/readyz")
    public Map<String, String> ready() {
        return Map.of("status", "ready");
    }
}
