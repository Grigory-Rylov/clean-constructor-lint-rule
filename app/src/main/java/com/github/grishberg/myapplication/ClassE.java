package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassE {
    private final ExpensiveClassC classC;

    @Inject
    public ClassE(ExpensiveClassC classC) {
        this.classC = classC;
    }
}
