package com.github.grishberg.myapplication;

public class ExpensiveJavaClass {
    public ExpensiveJavaClass() {
        expensiveMethod();
    }

    private void expensiveMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
