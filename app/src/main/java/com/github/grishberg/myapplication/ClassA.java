package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassA {
    private final ClassB classB;
    private final ClassC classC;

    @Inject
    public ClassA(ClassB classB, ClassC classC) {
        this.classB = classB;
        this.classC = classC;
    }
}
