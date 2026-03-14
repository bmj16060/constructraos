package net.mudpot.constructraos.apiservice.policy;

import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class DefaultPolicyInputBuilder implements PolicyInputBuilder {
    @Override
    public Map<String, Object> build(final MethodInvocationContext<Object, Object> context) {
        return Map.of();
    }
}
