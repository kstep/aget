package me.kstep.aget;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
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
import com.googlecode.androidannotations.annotations.sharedpreferences.Pref;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import android.app.PendingIntent;
import java.util.HashMap;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main_activity_actions)
public class MainActivity extends ListActivity implements DownloadItem.Listener {

    @Bean
    DownloadItemsAdapter adapter;

    @SystemService
    NotificationManager notifications;

    @Pref
    Preferences_ prefs;

    @AfterViews
    void bindAdapter() {
        setListAdapter(adapter);
        downloadNotifications = new HashMap<DownloadItem, Notification.Builder>();
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

    @AfterInject
    void setupDownloadPrefs() {
        DownloadItem.setConnectTimeout(prefs.connectTimeout().get());
        DownloadItem.setReadTimeout(prefs.readTimeout().get());
        DownloadItem.setDefaultContinue(prefs.continueDownload().get());
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

    //@OptionsItem
    //void downloadPrefs() {
        //getFragmentManager().beginTransaction()
            //.add(android.R.id.content, new PreferencesFragment_())
            //.addToBackStack(null)
            //.commit();
    //}

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            finish();
        }
    }

    @OptionsItem
    void home() {
        onBackPressed();
    }

    void downloadAdd(String url) {
        DownloadItem downloadItem = new DownloadItem();
        if (url != null) {
            try {
                downloadItem.setUrl(url)
                    .setFileName()
                    .setFileFolder();
            } catch (MalformedURLException e) {
            }
        }

        AddDownloadItemFragment addDownloadDialog = new AddDownloadItemFragment_();
        addDownloadDialog.bind(downloadItem);
        addDownloadDialog.show(getFragmentManager(), "addDownloadItem");
    }

    HashMap<DownloadItem,Notification.Builder> downloadNotifications;

    @UiThread
    public void downloadItemChanged(DownloadItem item) {
        adapter.notifyDataSetChanged();
        getListView().requestFocusFromTouch();

        DownloadItem.Status status = item.getStatus();
        int progress = item.getProgressInt();
        String statusName = (
                status == DownloadItem.Status.INITIAL? "… ":
                status == DownloadItem.Status.STARTED? "▶ ":
                status == DownloadItem.Status.PAUSED? "|| ":
                status == DownloadItem.Status.FINISHED? "✓ ":
                status == DownloadItem.Status.CANCELED? "■ ":
                status == DownloadItem.Status.FAILED? "✗ ":
                "? "
                );

        Notification.Builder notify;
        PendingIntent pi = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT
                );
        if (downloadNotifications.containsKey(item)) {
            notify = downloadNotifications.get(item);
        } else {
            notify = new Notification.Builder(this)
              .setSmallIcon(R.drawable.ic_launcher)
              .setContentIntent(pi)
              //.addAction(R.drawable.ic_action_settings, "Open", pi)
              //.addAction(R.drawable.ic_action_cancel, "Cancel", pi)
              ;
            downloadNotifications.put(item, notify);
        }

        notify.setContentTitle(statusName + item.getFileName())
              .setOngoing(status == DownloadItem.Status.STARTED)
              //.addAction(
                      //status == DownloadItem.Status.STARTED? R.drawable.ic_action_pause:
                      //status == DownloadItem.Status.FINISHED? R.drawable.ic_action_replay:
                      //R.drawable.ic_action_play,
                      //status == DownloadItem.Status.STARTED? "Pause":
                      //status == DownloadItem.Status.FINISHED? "Restart":
                      //"Start", pi)
              .setContentInfo(String.format("%s | %d%%",
                          Util.humanizeTime(item.getTimeLeft()),
                          progress
                          ))
              .setContentText(String.format("%s/%s @ %s/s",
                          Util.humanizeSize(item.getDownloadedSize()),
                          Util.humanizeSize(item.getTotalSize()),
                          Util.humanizeSize(item.getLastSpeed())
                          ))
              ;

        if (status == DownloadItem.Status.STARTED || status == DownloadItem.Status.PAUSED || status == DownloadItem.Status.FAILED) {
            notify.setProgress(100, progress, item.isUnkownSize() && status == DownloadItem.Status.STARTED);
        } else {
            notify.setProgress(0, 0, false);
        }

        notifications.notify(item.hashCode(), notify.build());
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
