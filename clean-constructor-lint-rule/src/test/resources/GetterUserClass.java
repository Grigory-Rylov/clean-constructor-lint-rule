package com.github.grishberg.delegateadapter;

public class GetterUserClass {
    private final int someField1;
    private final int someField2;
    private final int someField3;

    public GetterUserClass(SimpleProvider provider) {
        someField1 = provider.getIntValue();
        someField2 = provider.getConstValue();
        someField3 = getDefaultValue();
    }

    private int getDefaultValue() {
        return 1000;
    }
}