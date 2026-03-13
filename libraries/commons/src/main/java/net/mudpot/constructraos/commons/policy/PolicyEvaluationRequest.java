package net.mudpot.constructraos.commons.policy;

public record PolicyEvaluationRequest(
    String action,
    Object input
) {
}
