package net.mudpot.constructraos.orchestration;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(final String[] args) {
        Micronaut.build(args)
            .packages("net.mudpot.constructraos.orchestration")
            .mainClass(Application.class)
            .start();
    }
}
