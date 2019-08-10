package com.github.grishberg.delegateadapter;

import android.view.ViewGroup;

public interface AdapterDelegate<T, V extends T, VH extends RecycableViewHolder> {
    void onBindViewHolder(VH vh, V item);

    RecycableViewHolder onCreateViewHolder(ViewGroup parent);

    boolean isForType(T item);
}
