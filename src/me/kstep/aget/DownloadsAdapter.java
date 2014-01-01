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
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloadable;

@EBean
class DownloadsAdapter extends BaseAdapter {

    @RootContext
    Context context;

    @RootContext
    DownloadManagerActivity mainActivity;

    List<Download> items;

    @AfterInject
    public void initItems() {
        items = new LinkedList<Download>();
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
        DownloadView downloadView = view == null? DownloadView_.build(context): (DownloadView) view;
        final Download item = getItem(pos);
        downloadView.bind(item);
        return downloadView;
    }

    public void removeItem(Download item) {
        //try { item.stop(); } catch (IllegalStateException e) {}
        items.remove(item);
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        //try { getItem(pos).stop(); } catch (IllegalStateException e) {}
        items.remove(pos);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Download getItem(int pos) {
        return items.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    public void addItem(Download item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public void addItem(Downloadable item) {
        items.add(new Download(item));
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
