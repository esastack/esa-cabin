package io.esastack.cabin.sample.app;

import io.esastack.cabin.sample.agent.AgentClass;
import io.esastack.cabin.sample.lib.module.CabinTestLibModule;
import io.esastack.cabin.sample.lib.module2.CabinTestLibModule2;
import io.esastack.cabin.sample.lib.module3.CabinTestLibModule3;
import io.esastack.cabin.support.bootstrap.CabinAppBootstrap;

/**
 * Only for unit test, could not run in IDE, because could not found cabin-core jar in classpath
 */
public class CabinTestApp {

    public static void main(String[] args) {
        CabinAppBootstrap.run(args);
        CabinTestLibModule.echo();
        CabinTestLibModule2.echo();
        CabinTestLibModule3.echo();
        System.out.println(AgentClass.echo());
    }
}
