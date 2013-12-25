package me.kstep.aget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.googlecode.androidannotations.annotations.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.io.OptionalDataException;
import java.io.IOException;

@EBean
class DownloadItemsAdapter extends BaseAdapter {
    List<DownloadItem> items;

    @RootContext
    Context context;

    @RootContext
    MainActivity mainActivity;

    @AfterInject
    void initAdapter() {
        items = new LinkedList<DownloadItem>();
    }

    @SuppressWarnings("unchecked")
    public void loadFromStream(ObjectInputStream is) {
        try {
            items = (List<DownloadItem>) is.readObject();
            notifyDataSetChanged();
        } catch (OptionalDataException e) {
        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public void saveToStream(ObjectOutputStream os) {
        try {
            os.writeObject(items);
        } catch (IOException e) {
        }
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        DownloadItemView downloadView = view == null? DownloadItemView_.build(context): (DownloadItemView) view;
        final DownloadItem item = getItem(pos);
        downloadView.bind(item);
        return downloadView;
    }

    public void removeItem(DownloadItem item) {
        items.remove(item);
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        items.remove(pos);
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

    public void addItem(DownloadItem item) {
        if (!items.contains(item)) {
            items.add(item);
            notifyDataSetChanged();
        }
    }

    public void addItem(String url) {
        try {
            addItem(new DownloadItem(url));
        } catch (MalformedURLException e) {
        }
    }

    public void addItem(URL url) {
        addItem(new DownloadItem(url));
    }
}
