package net.mudpot.constructraos.commons.orchestration.policy.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;

@ActivityInterface
public interface PolicyEvaluationActivities {
    @ActivityMethod
    PolicyEvaluationResult evaluatePolicy(PolicyEvaluationRequest request);
}
