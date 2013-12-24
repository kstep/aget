package me.kstep.aget;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import com.googlecode.androidannotations.annotations.*;
import java.net.MalformedURLException;
import android.widget.Toast;

@EActivity(R.layout.main)
public class MainActivity extends Activity implements DownloadItem.Listener {

    @Bean
    DownloadItemsAdapter adapter;

    @ViewById
    ListView downloadList;

    @AfterViews
    void bindAdapter() {
        downloadList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.downloadAdd:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null) {
                    adapter.addItem(clip.getItemAt(0).coerceToText(this).toString());
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @UiThread
    public void downloadItemChanged(DownloadItem item) {
        adapter.notifyDataSetChanged();
        downloadList.requestFocusFromTouch();
    }

    @UiThread
    public void downloadItemFailed(DownloadItem item, Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Background
    void downloadStart(DownloadItem item) {
        item.fetchFileName();
        downloadItemChanged(item);
        item.startDownload(this);
    }

    void downloadCancel(DownloadItem item) {
        item.cancelDownload();
        adapter.removeItem(item);
        downloadList.requestFocusFromTouch();
    }

}
