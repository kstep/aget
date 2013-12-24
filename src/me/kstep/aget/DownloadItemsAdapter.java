package me.kstep.aget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.googlecode.androidannotations.annotations.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.LinkedList;

@EBean
class DownloadItemsAdapter extends BaseAdapter {
    List<DownloadItem> items;

    @RootContext
    Context context;

    @AfterInject
    void initAdapter() {
        items = new LinkedList<DownloadItem>();
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        DownloadItemView downloadView = view == null? DownloadItemView_.build(context): (DownloadItemView) view;
        downloadView.bind(getItem(pos));
        return downloadView;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public DownloadItem getItem(int pos) {
        return items.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    private String humanize(long value) {
        String[] suffixes = {"b", "K", "M", "G", "T"};
        String suffix;
        int index = 0;

        while (value > 1024 && index < suffixes.length) {
            value /= 1024;
            index++;
        }

        return value + suffixes[index];
    }

    public void add(DownloadItem item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public void add(String url) {
        try {
            add(new DownloadItem(url));
        } catch (MalformedURLException e) {
        }
    }

    public void add(URL url) {
        add(new DownloadItem(url));
    }
}
