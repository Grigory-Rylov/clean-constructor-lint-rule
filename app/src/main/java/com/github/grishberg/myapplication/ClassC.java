package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassC {
    private final ExpensiveClassA classA;

    @Inject
    public ClassC(ExpensiveClassA classA) {
        this.classA = classA;
    }
}
