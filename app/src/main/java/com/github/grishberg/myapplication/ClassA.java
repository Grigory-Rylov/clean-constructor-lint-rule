package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassA {
    private final ClassB classB;

    @Inject
    public ClassA(ClassB classB, ClassC classC) {
        this.classB = classB;
    }
}
