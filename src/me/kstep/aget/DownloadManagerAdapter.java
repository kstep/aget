package me.kstep.aget;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.googlecode.androidannotations.annotations.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.LinkedList;
import java.util.List;
import me.kstep.downloader.DownloadManager;

@EBean
class DownloadManagerAdapter extends BaseAdapter {
    @Bean
    DownloadManager manager;

    @RootContext
    Context context;

    @RootContext
    MainActivity mainActivity;

    List<Download> items;

    @AfterInject
    public void setupItems() {
        items = manager.getJobs();
    }

    @SuppressWarnings("unchecked")
    public void loadFromStream(ObjectInputStream is) {
        //try {
            //items = (List<DownloadItem>) is.readObject();
            //notifyDataSetChanged();
        //} catch (OptionalDataException e) {
        //} catch (ClassNotFoundException e) {
        //} catch (IOException e) {
        //}
    }

    public void saveToStream(ObjectOutputStream os) {
        //try {
            //os.writeObject(items);
        //} catch (IOException e) {
        //}
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        DownloadItemView downloadView = view == null? DownloadItemView_.build(context): (DownloadItemView) view;
        final DownloadItem item = getItem(pos);
        downloadView.bind(item);
        return downloadView;
    }

    public void removeItem(Download item) {
        manager.dequeue(item);
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        manager.dequeue(pos);
        notifyDataSetChanged();
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

    public Download addItem(DownloadItem item) {
        Download d = manager.enqueue(item);
        notifyDataSetChanged();
        return d;
    }

    public Download addItem(Download item) {
        item = manager.enqueue(item);
        notifyDataSetChanged();
        return item;
    }

    public Download addItem(String url) {
        try {
            return addItem(new DownloadItem(url));
        } catch (NullPointerException e) {
        }
    }

    public Download addItem(Uri uri) {
        return addItem(new DownloadItem(uri));
    }
}
