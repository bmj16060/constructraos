package net.mudpot.constructraos.apiservice;

import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(final String[] args) {
        Micronaut.build(args)
            .packages("net.mudpot.constructraos.apiservice")
            .mainClass(Application.class)
            .start();
    }
}
