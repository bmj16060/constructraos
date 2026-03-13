package net.mudpot.constructraos.commons.policy;

public interface PolicyEvaluator {
    PolicyEvaluationResult evaluate(PolicyEvaluationRequest request);
}
