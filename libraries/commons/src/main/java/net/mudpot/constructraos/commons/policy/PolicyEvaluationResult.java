package net.mudpot.constructraos.commons.policy;

public record PolicyEvaluationResult(
    boolean allowed,
    String reason,
    String policyVersion
) {
}
