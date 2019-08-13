package com.github.grishberg.delegateadapter;

public class SimpleProvider {
    private final int intValue;

    public int getIntValue() {
        return intValue;
    }

    public int getConstValue() {
        return 1000;
    }
}