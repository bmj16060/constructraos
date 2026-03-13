package net.mudpot.constructraos.orchestration.core.policy;

import io.temporal.failure.ApplicationFailure;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;

public final class WorkflowPolicyEnforcer {
    private static final String FAILURE_TYPE_PREFIX = "workflow_policy_denied";

    private WorkflowPolicyEnforcer() {
    }

    public static void enforce(
        final PolicyEvaluationActivities policyActivities,
        final String action,
        final Object payload
    ) {
        final PolicyEvaluationResult decision = policyActivities.evaluatePolicy(
            new PolicyEvaluationRequest(action, payload)
        );
        if (decision.allowed()) {
            return;
        }

        final ApplicationFailure failure = ApplicationFailure.newNonRetryableFailure(
            "Denied by workflow policy: " + safeReason(decision.reason()) + ".",
            FAILURE_TYPE_PREFIX + ":" + safeReason(decision.reason()),
            safeReason(decision.reason()),
            safeVersion(decision.policyVersion())
        );
        failure.setStackTrace(new StackTraceElement[0]);
        throw failure;
    }

    private static String safeReason(final String value) {
        if (value == null || value.isBlank()) {
            return "unknown_policy_reason";
        }
        return value.trim();
    }

    private static String safeVersion(final String value) {
        if (value == null || value.isBlank()) {
            return "unknown_policy_version";
        }
        return value.trim();
    }
}
