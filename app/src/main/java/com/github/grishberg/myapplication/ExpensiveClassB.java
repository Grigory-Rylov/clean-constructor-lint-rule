package com.github.grishberg.myapplication;

public class ExpensiveClassB {
    private void expensiveInitialMethod() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
