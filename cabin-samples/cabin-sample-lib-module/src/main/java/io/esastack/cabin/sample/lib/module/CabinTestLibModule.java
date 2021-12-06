package io.esastack.cabin.sample.lib.module;

import io.esastack.cabin.sample.lib.module3.CabinTestLibModule3;

public class CabinTestLibModule {

    public static void echo() {
        System.out.println("load CabinTestLibModule3 from module1:" + CabinTestLibModule3.class.getClassLoader());
        System.out.println("load CabinTestLibModule from module1:" + CabinTestLibModule.class.getClassLoader());
    }
}
