package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class ClassD {
    private final ClassE classE;

    @Inject
    public ClassD(ClassE classE) {
        this.classE = classE;
    }
}
