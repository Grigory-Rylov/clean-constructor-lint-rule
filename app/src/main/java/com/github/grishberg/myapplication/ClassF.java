package com.github.grishberg.myapplication;

public class ClassF {
    public static void someMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
