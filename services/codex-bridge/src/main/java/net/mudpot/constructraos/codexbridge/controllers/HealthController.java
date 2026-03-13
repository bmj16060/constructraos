package net.mudpot.constructraos.codexbridge.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/healthz")
public class HealthController {
    @Get
    public HttpResponse<String> health() {
        return HttpResponse.ok("ok");
    }
}
