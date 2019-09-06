package com.github.grishberg.delegateadapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SampleInnerClass {

    public SampleInnerClass(View v) {
        InnerClass innerClass = new InnerClass();
    }

    public void onRecycled() {
        /* to be implemented in subclass */
    }

    public void onAtachedToWindow() {
        /* to be implemented in subclass */
    }

    public void onDetachedFromWindow() {
        /* to be implemented in subclass */
    }

    private class InnerClass {
        InnerClass() {
            doSomeExpensiveMethod();
        }

        private void doSomeExpensiveMethod() {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }
}
