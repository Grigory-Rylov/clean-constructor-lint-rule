package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassC {
    private final ExpensiveClassA classA;

    @Inject
    public ClassC(ExpensiveClassA classA) {
        this.classA = classA;
    }

    public void someMethod() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
