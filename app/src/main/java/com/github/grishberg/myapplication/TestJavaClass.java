package com.github.grishberg.myapplication;

import javax.inject.Inject;

public class TestJavaClass {
    private ExpensiveJavaClass listener;

    @Inject
    public TestJavaClass(ExpensiveJavaClass expensiveConstructorsArgument) {
        listener = expensiveConstructorsArgument;
    }

    public void x() {
        listener.expensiveMethod();
    }
}
