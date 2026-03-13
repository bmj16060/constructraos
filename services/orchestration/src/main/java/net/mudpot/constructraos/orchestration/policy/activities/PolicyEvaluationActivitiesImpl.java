package net.mudpot.constructraos.orchestration.policy.activities;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.orchestration.policy.client.PolicyServiceHttpClient;

@Singleton
public class PolicyEvaluationActivitiesImpl implements PolicyEvaluationActivities {
    private final PolicyServiceHttpClient policyServiceHttpClient;
    private final boolean enforce;

    public PolicyEvaluationActivitiesImpl(
        final PolicyServiceHttpClient policyServiceHttpClient,
        @Value("${policy.service-enforce:true}") final boolean enforce
    ) {
        this.policyServiceHttpClient = policyServiceHttpClient;
        this.enforce = enforce;
    }

    @Override
    public PolicyEvaluationResult evaluatePolicy(final PolicyEvaluationRequest request) {
        try {
            return policyServiceHttpClient.evaluate(request);
        } catch (final Exception exception) {
            if (!enforce) {
                return new PolicyEvaluationResult(true, "policy-service-error-fail-open", "policy-service");
            }
            throw new RuntimeException("Policy evaluation activity failed", exception);
        }
    }
}
