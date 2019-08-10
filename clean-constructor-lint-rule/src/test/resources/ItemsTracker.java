package com.github.grishberg.delegateadapter;

import android.support.v7.widget.RecyclerView;

public interface ItemsTracker {
    void startTracking(final RecyclerView rv);

    void clear();

    ItemsTracker STUB = new ItemsTracker() {
        @Override
        public void startTracking(RecyclerView rv) {
            /* stub */
        }

        @Override
        public void clear() {
            /* stub */
        }
    };
}
