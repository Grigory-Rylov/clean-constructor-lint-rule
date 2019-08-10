package com.github.grishberg.delegateadapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter with composition of delegate.
 */
public class CompositeDelegateAdapter<T extends ItemWithId> extends RecyclerView.Adapter<com.github.grishberg.delegateadapter.RecycableViewHolder> {
    private final ArrayList<T> items = new ArrayList<>();
    private final Delegates<T> delegates;

    public CompositeDelegateAdapter(List<AdapterDelegate> delegates) {
        this.delegates = new Delegates<>(delegates);
    }

    /**
     * Populate adapter with data.
     * Should be called before notifyDataSetChanged or DiffUtils.
     */
    public void populate(List<T> i) {
        items.clear();
        items.addAll(i);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @NonNull
    @Override
    public com.github.grishberg.delegateadapter.RecycableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return delegates.createVh(parent, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull com.github.grishberg.delegateadapter.RecycableViewHolder vh, int pos) {
        T item = items.get(pos);
        delegates.bindVh(vh, item, pos);
    }

    @Override
    public int getItemViewType(int position) {
        return delegates.getViewType(items, position);
    }

    @Override
    public void onViewRecycled(@NonNull com.github.grishberg.delegateadapter.RecycableViewHolder holder) {
        delegates.onViewRecycled(holder);
    }
}
