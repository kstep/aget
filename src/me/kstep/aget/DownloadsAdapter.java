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
import java.util.List;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloadable;

@EBean
class DownloadsAdapter extends BaseAdapter {

    @RootContext
    Context context;

    @RootContext
    DownloadManagerActivity mainActivity;

    List<Download> items = null;

    public void setDownloadList(List<Download> list) {
        items = list;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        DownloadView downloadView = view == null? DownloadView_.build(context): (DownloadView) view;
        final Download item = getItem(pos);
        downloadView.bind(item);
        return downloadView;
    }

    public void removeItem(Download item) {
        if (items != null) {
            //try { item.stop(); } catch (IllegalStateException e) {}
            items.remove(item);
            notifyDataSetChanged();
        }
    }

    public void removeItem(int pos) {
        if (items != null) {
            //try { getItem(pos).stop(); } catch (IllegalStateException e) {}
            items.remove(pos);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return items == null? 0: items.size();
    }

    @Override
    public Download getItem(int pos) {
        return items == null? null: items.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    public void addItem(Download item) {
        if (items != null) {
            items.add(item);
            notifyDataSetChanged();
        }
    }

    public void addItem(Downloadable item) {
        addItem(new Download(item));
    }

    public void addItem(String url) {
        try {
            addItem(new DownloadItem(url));
        } catch (NullPointerException e) {
        }
    }

    public void addItem(Uri uri) {
        addItem(new DownloadItem(uri));
    }
}
