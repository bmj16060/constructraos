package net.mudpot.constructraos.policyservice;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(final String[] args) {
        Micronaut.build(args)
            .packages("net.mudpot.constructraos.policyservice")
            .mainClass(Application.class)
            .start();
    }
}
