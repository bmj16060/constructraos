package net.mudpot.constructraos.apiservice.policy;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.util.Map;

@Singleton
@InterceptorBean(AuthPolicy.class)
@ExecuteOn(TaskExecutors.BLOCKING)
public class AuthPolicyInterceptor implements MethodInterceptor<Object, Object> {
    private final PolicyEvaluator policyEvaluator;
    private final BeanContext beanContext;

    public AuthPolicyInterceptor(
        final PolicyEvaluator policyEvaluator,
        final BeanContext beanContext
    ) {
        this.policyEvaluator = policyEvaluator;
        this.beanContext = beanContext;
    }

    @Override
    public Object intercept(final MethodInvocationContext<Object, Object> context) {
        final String action = context.stringValue(AuthPolicy.class).orElse("").trim();
        if (action.isBlank()) {
            return context.proceed();
        }

        final Class<? extends PolicyInputBuilder> inputBuilderType = context.classValue(AuthPolicy.class, "inputBuilder")
            .map(type -> type.asSubclass(PolicyInputBuilder.class))
            .orElse(DefaultPolicyInputBuilder.class);
        final PolicyInputBuilder inputBuilder = beanContext.getBean(inputBuilderType);
        final Map<String, Object> input = inputBuilder.build(context);

        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Policy denied: " + result.reason());
        }
        return context.proceed();
    }
}
