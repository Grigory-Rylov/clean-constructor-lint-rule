package com.github.grishberg.delegateadapter;

import android.support.v7.widget.RecyclerView;

import com.github.grishberg.delegateadapter.AdapterDelegate;
import com.github.grishberg.delegateadapter.CompositeDelegateAdapter;

import java.util.List;

public class ItemsAdapter {
    private final CompositeDelegateAdapter<Item> adapter;

    public ItemsAdapter(List<AdapterDelegate> delegates) {
        adapter = new CompositeDelegateAdapter<>(delegates);
    }

    public void attachToRecyclerView(RecyclerView rv) {
        rv.setAdapter(adapter);
    }

    public void populate(List<Item> items) {
        adapter.populate(items);
    }
}
