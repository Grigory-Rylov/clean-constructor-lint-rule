package com.github.grishberg.delegateadapter;

import android.support.v4.util.SparseArrayCompat;
import android.view.ViewGroup;

import java.util.List;

class Delegates<T> {
    private final SparseArrayCompat<AdapterDelegate> delegates = new SparseArrayCompat<>();

    Delegates(List<AdapterDelegate> delegates) {
        int type = 0;
        for (AdapterDelegate d : delegates) {
            this.delegates.append(type++, d);
        }
    }

    @SuppressWarnings("unchecked")
    int getViewType(List<T> items, int pos) {
        for (int i = 0, l = delegates.size(); i < l; i++) {
            if (delegates.get(i).isForType(items.get(pos))) {
                return i;
            }
        }
        throw new IllegalStateException("Delegate not found");
    }

    @SuppressWarnings("unchecked")
    void bindVh(RecycableViewHolder vh, T item, int pos) {
        for (int i = 0, l = delegates.size(); i < l; i++) {
            AdapterDelegate adapterDelegate = delegates.get(i);
            if (adapterDelegate.isForType(item)) {
                adapterDelegate.onBindViewHolder(vh, item);
                return;
            }
        }
        throw new IllegalStateException("Not found delegate for position = " + pos);
    }

    RecycableViewHolder createVh(ViewGroup parent, int viewType) {
        AdapterDelegate d = delegates.get(viewType);
        if (d == null) {
            throw new IllegalStateException("Not found delegate for viewType = " + viewType);
        }
        return d.onCreateViewHolder(parent);
    }

    void onViewRecycled(RecycableViewHolder vh) {
        vh.onRecycled();
    }
}
