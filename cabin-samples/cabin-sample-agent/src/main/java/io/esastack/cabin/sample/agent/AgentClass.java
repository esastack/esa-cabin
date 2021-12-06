package io.esastack.cabin.sample.agent;

import io.esastack.cabin.sample.lib.module.CabinTestLibModule;

public class AgentClass {

    public static String echo() {
        return "load from agent: " + CabinTestLibModule.class.getName();
    }
}
