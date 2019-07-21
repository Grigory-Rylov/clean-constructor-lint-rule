package com.github.grishberg.myapplication;

import android.content.Context;

public class TestJavaClass {
    private ExpensiveJavaClass listener;

    public TestJavaClass(Context context) {
        listener = new ExpensiveJavaClass(context);
    }
}
