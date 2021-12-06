package io.esastack.cabin.sample.lib.module3;


public class CabinTestLibModule3 {

    public static void echo() {
        System.out.println("load CabinTestLibModule3 from module3:" + CabinTestLibModule3.class.getClassLoader());
    }
}
