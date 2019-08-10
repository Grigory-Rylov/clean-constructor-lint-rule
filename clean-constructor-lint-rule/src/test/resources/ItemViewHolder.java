package com.grishberg.listarch.listarchitecture.rv;

import android.view.*;
import android.widget.*;

import com.github.grishberg.delegateadapter.RecycableViewHolder;
import com.grishberg.listarch.listarchitecture.*;

class ItemViewHolder extends RecycableViewHolder {
    private final TextView tv;

    ItemViewHolder(View v) {
        super(v);
        tv = v.findViewById(R.id.title);
    }

    void setTitle(String t) {
        tv.setText(t);
    }
}
