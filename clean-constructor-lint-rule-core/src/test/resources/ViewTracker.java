package com.github.grishberg.delegateadapter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;

public class ViewTracker implements ItemsTracker {
    private static final String TAG = ViewTracker.class.getSimpleName();
    private int prevStartPos = -1;
    private final SparseIntArray counts = new SparseIntArray();

    public void startTracking(final RecyclerView rv) {
        if (!(rv.getLayoutManager() instanceof LinearLayoutManager)) {
            return;
        }

        if (!(rv.getAdapter() instanceof CompositeDelegateAdapter)) {
            return;
        }

        final CompositeDelegateAdapter adapter = (CompositeDelegateAdapter) rv.getAdapter();
        final LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                scroll(adapter, lm);
            }
        });
    }

    private void scroll(CompositeDelegateAdapter adapter, LinearLayoutManager lm) {
        int startPos = lm.findFirstCompletelyVisibleItemPosition();
        int endPos = lm.findLastCompletelyVisibleItemPosition();

        if (startPos == prevStartPos || startPos == -1) {
            return;
        }
        prevStartPos = startPos;
        for (int i = startPos; i <= endPos; i++) {
            if (i >= adapter.getItemCount()) {
                Log.e(TAG, "pos = " + i +" is greater then adapter count " + adapter.getItemCount());
                return;
            }

            int id = (int) adapter.getItemId(i);
            counts.put(id, counts.get(id) + 1);
        }
    }

    @Override
    public void clear() {
        counts.clear();
    }
}
