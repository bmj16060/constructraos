package net.mudpot.constructraos.codexbridge;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(final String[] args) {
        Micronaut.build(args)
            .packages("net.mudpot.constructraos.codexbridge")
            .mainClass(Application.class)
            .start();
    }
}
