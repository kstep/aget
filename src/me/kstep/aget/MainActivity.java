package me.kstep.aget;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.googlecode.androidannotations.annotations.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main_activity_actions)
public class MainActivity extends ListActivity implements DownloadItem.Listener {

    @Bean
    DownloadItemsAdapter adapter;

    @AfterViews
    void bindAdapter() {
        setListAdapter(adapter);
    }

    @AfterViews
    void processIntentUri() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            downloadAdd(uri.toString());
        }
    }

    @AfterInject
    void loadDownloadsList() {
        try {
            ObjectInputStream io = new ObjectInputStream(openFileInput("downloadItems.bin"));
            adapter.loadFromStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            ObjectOutputStream io = new ObjectOutputStream(openFileOutput("downloadItems.bin", MODE_PRIVATE));
            adapter.saveToStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    @OptionsItem
    void downloadAdd() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
        ClipData clip = clipboard.getPrimaryClip();
        String uri = clip == null? null: clip.getItemAt(0).coerceToText(this).toString();
        downloadAdd(uri);
    }

    void downloadAdd(String url) {
        DownloadItem downloadItem = new DownloadItem();
        if (url != null) {
            try {
                downloadItem.setUrl(url).setFileName();
            } catch (MalformedURLException e) {
            }
        }

        AddDownloadItemFragment addDownloadDialog = new AddDownloadItemFragment_();
        addDownloadDialog.bind(downloadItem);
        addDownloadDialog.show(getFragmentManager(), "addDownloadItem");
    }

    @UiThread
    public void downloadItemChanged(DownloadItem item) {
        adapter.notifyDataSetChanged();
        getListView().requestFocusFromTouch();
    }

    @UiThread
    public void downloadItemFailed(DownloadItem item, Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @ItemClick
    public void listItemClicked(DownloadItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(item.getFile()));
        startActivity(intent);
    }
}
