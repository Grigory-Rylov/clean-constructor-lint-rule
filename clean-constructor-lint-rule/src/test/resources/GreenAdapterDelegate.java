package com.grishberg.listarch.listarchitecture.rv;

import android.view.*;

import com.github.grishberg.delegateadapter.AdapterDelegate;
import com.github.grishberg.delegateadapter.RecycableViewHolder;
import com.grishberg.listarch.listarchitecture.*;

public class GreenAdapterDelegate implements AdapterDelegate<Item, GreenItem, ItemViewHolder> {
    private final LayoutInflater inflater;

    public GreenAdapterDelegate(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public boolean isForType(Item item) {
        return !item.isRed();
    }

    @Override
    public RecycableViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ItemViewHolder(inflater.inflate(R.layout.green_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder vh, GreenItem item) {
        item.doSpecialForGreen();
        item.renderToViewHolder(vh);
    }
}
