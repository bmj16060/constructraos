package net.mudpot.constructraos.policyservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.util.Map;

@Singleton
public class OpaSidecarClient implements PolicyEvaluator {
    private final ObjectMapper objectMapper;
    private final OpaSidecarHttpClient opaSidecarHttpClient;

    public OpaSidecarClient(final ObjectMapper objectMapper, final OpaSidecarHttpClient opaSidecarHttpClient) {
        this.objectMapper = objectMapper;
        this.opaSidecarHttpClient = opaSidecarHttpClient;
    }

    @Override
    public PolicyEvaluationResult evaluate(final PolicyEvaluationRequest request) {
        try {
            final JsonNode inputNode = request.input() == null
                ? objectMapper.createObjectNode()
                : objectMapper.valueToTree(request.input());
            if (inputNode instanceof ObjectNode objectNode && request.action() != null && !request.action().isBlank()) {
                objectNode.put("action", request.action());
            }
            final Map<String, Object> response = opaSidecarHttpClient.evaluate(Map.of("input", objectMapper.treeToValue(inputNode, Object.class)));
            final JsonNode result = objectMapper.valueToTree(response == null ? Map.of() : response).path("result");
            return new PolicyEvaluationResult(
                result.path("allow").asBoolean(false),
                result.path("reason").asText("policy-deny"),
                result.path("policy_version").asText("constructraos.v1")
            );
        } catch (final Exception exception) {
            return new PolicyEvaluationResult(false, "opa-error", "constructraos.v1");
        }
    }
}
