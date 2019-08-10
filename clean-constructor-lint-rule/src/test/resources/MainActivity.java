package com.grishberg.listarch.listarchitecture;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.github.grishberg.consoleview.Logger;
import com.github.grishberg.consoleview.LoggerImpl;
import com.github.grishberg.delegateadapter.AdapterDelegate;
import com.github.grishberg.delegateadapter.ItemsTracker;
import com.github.grishberg.delegateadapter.ViewTracker;
import com.grishberg.listarch.listarchitecture.rv.CustomItemDecorator;
import com.grishberg.listarch.listarchitecture.rv.GreenAdapterDelegate;
import com.grishberg.listarch.listarchitecture.rv.GreenItem;
import com.grishberg.listarch.listarchitecture.rv.Item;
import com.grishberg.listarch.listarchitecture.rv.ItemsAdapter;
import com.grishberg.listarch.listarchitecture.rv.ItemsRecyclerView;
import com.grishberg.listarch.listarchitecture.rv.RedItem;
import com.grishberg.listarch.listarchitecture.rv.RedItemsAdapterDelegate;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Logger log;
    private ItemsAdapter adapter;
    private ItemsRecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        log = new LoggerImpl();
        rv = findViewById(R.id.rv);

        LayoutInflater inflater = LayoutInflater.from(this);
        ArrayList<AdapterDelegate> delegates = new ArrayList<>();
        delegates.add(new RedItemsAdapterDelegate(inflater));
        delegates.add(new GreenAdapterDelegate(inflater));
        adapter = new ItemsAdapter(delegates);

        rv.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        adapter.attachToRecyclerView(rv);
        adapter.populate(createData());

        LinearLayoutManager lm = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        rv.setLayoutManager(lm);
        CustomItemDecorator itemDecorator = new CustomItemDecorator(
                getResources().getDimensionPixelSize(R.dimen.sideOffset),
                getResources().getDimensionPixelSize(R.dimen.midOffset));
        rv.addItemDecoration(itemDecorator);
        rv.scrollToPosition(3);

        ItemsTracker tracker = new ViewTracker();
        tracker.startTracking(rv);
    }

    private List<Item> createData() {
        ArrayList<Item> res = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                res.add(new RedItem(i, "Menu item " + i));
            } else {
                res.add(new GreenItem(i, "Menu item " + i));
            }
        }
        return res;
    }
}
