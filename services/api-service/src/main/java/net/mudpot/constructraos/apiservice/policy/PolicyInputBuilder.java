package net.mudpot.constructraos.apiservice.policy;

import io.micronaut.aop.MethodInvocationContext;

import java.util.Map;

public interface PolicyInputBuilder {
    Map<String, Object> build(MethodInvocationContext<Object, Object> context);
}
