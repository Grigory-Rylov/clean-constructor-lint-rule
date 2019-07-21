package com.github.grishberg.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpensiveJavaClass {
    private View.OnClickListener listener;

    public ExpensiveJavaClass(Context context) {
        Map m = new HashMap();
        m.put("1", 1);
        LayoutInflater mInflater = LayoutInflater.from(context);
        List l = new ArrayList();
        l.add("1");
        expensiveMethod();
        listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        };
    }

    private void expensiveMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
