package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassA {
    private final ClassB classB;
    private final ClassC classC;
    private final ClassF classF;

    @Inject
    public ClassA(ClassF classF, ClassB classB, ClassC classC) {
        this.classB = classB;
        this.classC = classC;
        this.classF = classF;
        ClassF.someMethod();
    }
}
