package net.mudpot.constructraos.policyservice;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import java.util.Map;

@Client("${policy.opa-url:`http://127.0.0.1:8181`}")
public interface OpaSidecarHttpClient {
    @Post("/v1/data/constructraos/authz")
    Map<String, Object> evaluate(@Body Map<String, Object> request);
}
