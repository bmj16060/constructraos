package net.mudpot.constructraos.orchestration.core.temporal;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import jakarta.inject.Named;
import net.mudpot.constructraos.commons.orchestration.ai.activities.LlmActivities;
import net.mudpot.constructraos.commons.orchestration.codex.activities.CodexExecutionActivities;
import net.mudpot.constructraos.commons.orchestration.policy.activities.PolicyEvaluationActivities;
import net.mudpot.constructraos.commons.orchestration.ai.activities.PromptActivities;
import net.mudpot.constructraos.commons.orchestration.system.activities.HelloActivities;

import java.time.Duration;

@Factory
public class TemporalWorkflowStubFactory {
    @Prototype
    @Named("helloActivitiesStub")
    HelloActivities helloActivitiesStub() {
        return Workflow.newActivityStub(
            HelloActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build()
        );
    }

    @Prototype
    @Named("codexExecutionActivitiesStub")
    CodexExecutionActivities codexExecutionActivitiesStub() {
        return Workflow.newActivityStub(
            CodexExecutionActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(20))
                        .setMaximumAttempts(2)
                        .build()
                )
                .build()
        );
    }

    @Prototype
    @Named("promptActivitiesStub")
    PromptActivities promptActivitiesStub() {
        return Workflow.newActivityStub(PromptActivities.class, externalOptions());
    }

    @Prototype
    @Named("llmActivitiesStub")
    LlmActivities llmActivitiesStub() {
        return Workflow.newActivityStub(
            LlmActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(20))
                        .setMaximumAttempts(2)
                        .build()
                )
                .build()
        );
    }

    @Prototype
    @Named("policyEvaluationActivitiesStub")
    PolicyEvaluationActivities policyEvaluationActivitiesStub() {
        return Workflow.newActivityStub(
            PolicyEvaluationActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(15))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(5))
                        .setMaximumAttempts(3)
                        .build()
                )
                .build()
        );
    }

    private static ActivityOptions externalOptions() {
        return ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(20))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setBackoffCoefficient(2.0)
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setMaximumAttempts(3)
                    .build()
            )
            .build();
    }
}
