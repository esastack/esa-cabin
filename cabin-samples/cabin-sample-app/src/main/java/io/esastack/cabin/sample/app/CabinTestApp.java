package io.esastack.cabin.sample.app;

import io.esastack.cabin.sample.lib.module.CabinTestLibModule;
import io.esastack.cabin.support.bootstrap.CabinAppBootstrap;

/**
 * Only for unit test, could not run in IDE, because could not found cabin-core jar in classpath
 */
public class CabinTestApp {

    public static void main(String[] args) {
        CabinAppBootstrap.run(args);
        System.out.println(CabinTestLibModule.getClassLoader());
    }
}
