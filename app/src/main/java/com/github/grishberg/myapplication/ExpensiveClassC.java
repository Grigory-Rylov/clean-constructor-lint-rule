package com.github.grishberg.myapplication;

public class ExpensiveClassC {
    private final ExpensiveClassA expensiveClassA;
    private final ExpensiveClassB expensiveClassB;

    public ExpensiveClassC() {
        this.expensiveClassA = new ExpensiveClassA();
        this.expensiveClassB = new ExpensiveClassB();
    }
}
