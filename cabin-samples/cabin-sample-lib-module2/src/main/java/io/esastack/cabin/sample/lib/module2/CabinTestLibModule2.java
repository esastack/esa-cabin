package io.esastack.cabin.sample.lib.module2;

import io.esastack.cabin.sample.lib.module.CabinTestLibModule;

public class CabinTestLibModule2 {

    public static void echo() {
        System.out.println("load CabinTestLibModule from module2:" + CabinTestLibModule.class.getClassLoader());
        System.out.println("load CabinTestLibModule2 from module2:" + CabinTestLibModule2.class.getClassLoader());
    }
}
