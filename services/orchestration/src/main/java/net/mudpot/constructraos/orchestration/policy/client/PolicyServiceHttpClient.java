package net.mudpot.constructraos.orchestration.policy.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;

@Client("${policy.service-url}")
public interface PolicyServiceHttpClient {
    @Post("/v1/policy/evaluate")
    PolicyEvaluationResult evaluate(@Body PolicyEvaluationRequest request);
}
