package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassB {
    private final ClassC classC;

    @Inject
    public ClassB(ClassC classC) {
        this.classC = classC;
    }
}
