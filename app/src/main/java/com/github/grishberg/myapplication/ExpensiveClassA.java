package com.github.grishberg.myapplication;

public class ExpensiveClassA {

    public ExpensiveClassA() {
        expensiveInitialMethod();
    }

    private void expensiveInitialMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
