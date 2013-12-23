package me.kstep.aget;

import android.app.DownloadManager;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.googlecode.androidannotations.annotations.*;
import java.net.MalformedURLException;
import android.view.View;

@EActivity(R.layout.main)
public class MainActivity extends ListActivity implements DownloadItem.Listener {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        DownloadItem[] items = new DownloadItem[1];
        DownloadItem item;
        try {
            items[0] = new DownloadItem("http://192.168.0.105:8080/Elementary.S02E10.rus.LostFilm.TV.avi");
        } catch (MalformedURLException e) {
        }

        ListAdapter adapter = new DownloadItemsAdapter(this, items);
        setListAdapter(adapter);
    }

    @Click
    public void downloadStart(View button) {
        android.util.Log.d("aGet", "downloadStart clicked");
        DownloadItem item = (DownloadItem) ((View) button.getParent()).getTag();
        downloadItem(item);
    }

    @Click
    public void downloadCancel(View button) {
        DownloadItem item = (DownloadItem) ((View) button.getParent()).getTag();
        item.setStatus(DownloadItem.Status.CANCELED);
    }

    @Click
    public void downloadPause(View button) {
        DownloadItem item = (DownloadItem) ((View) button.getParent()).getTag();
        item.setStatus(DownloadItem.Status.PAUSED);
    }

    @UiThread
    public void downloadItemChanged(DownloadItem item) {
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
        getListView().requestFocusFromTouch();
    }

    @Background
    void downloadItem(DownloadItem item) {
        item.fetchFileName();
        downloadItemChanged(item);
        android.util.Log.d("aGet", "name updated, downloading...");
        item.download(this);
    }

}
