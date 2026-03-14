package net.mudpot.constructraos.apiservice.policy;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.session.AnonymousSessionService;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationRequest;
import net.mudpot.constructraos.commons.policy.PolicyEvaluationResult;
import net.mudpot.constructraos.commons.policy.PolicyEvaluator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@InterceptorBean(AuthPolicy.class)
@ExecuteOn(TaskExecutors.BLOCKING)
public class AuthPolicyInterceptor implements MethodInterceptor<Object, Object> {
    private final PolicyEvaluator policyEvaluator;
    private final AnonymousSessionService anonymousSessionService;

    public AuthPolicyInterceptor(
        final PolicyEvaluator policyEvaluator,
        final AnonymousSessionService anonymousSessionService
    ) {
        this.policyEvaluator = policyEvaluator;
        this.anonymousSessionService = anonymousSessionService;
    }

    @Override
    public Object intercept(final MethodInvocationContext<Object, Object> context) {
        final String action = context.stringValue(AuthPolicy.class).orElse("").trim();
        if (action.isBlank()) {
            return context.proceed();
        }

        final Map<String, Object> input = new LinkedHashMap<>();
        input.put("resource", "api");
        input.put("action", action);
        input.put("arguments", Map.of());

        final Optional<HttpRequest<Object>> currentRequest = ServerRequestContext.currentRequest();
        if (currentRequest.isPresent()) {
            final HttpRequest<Object> request = currentRequest.get();
            final var session = anonymousSessionService.ensureSession(request);
            input.put("actor", Map.of(
                "kind", session.actorKind(),
                "session_id", session.sessionId()
            ));
            input.put("request", Map.of(
                "method", request.getMethodName(),
                "path", request.getPath(),
                "query", requestQuery(request)
            ));
        }

        final PolicyEvaluationResult result = policyEvaluator.evaluate(new PolicyEvaluationRequest(action, input));
        if (!result.allowed()) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Policy denied: " + result.reason());
        }
        return context.proceed();
    }

    private static Map<String, Object> requestQuery(final HttpRequest<Object> request) {
        final Map<String, Object> query = new LinkedHashMap<>();
        request.getParameters().forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                return;
            }
            query.put(name, values.size() == 1 ? values.getFirst() : List.copyOf(values));
        });
        return query;
    }
}
