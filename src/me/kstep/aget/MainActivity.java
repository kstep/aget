package me.kstep.aget;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.HashMap;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloader;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main_activity_actions)
public class MainActivity extends ListActivity implements Download.Listener {

    @Bean
    DownloadManagerAdapter adapter;

    @SystemService
    NotificationManager notifications;

    @Pref
    Preferences_ prefs;

    @Extra(Intent.EXTRA_TEXT)
    String sharedText;

    @AfterViews
    void bindAdapter() {
        setListAdapter(adapter);
        downloadNotifications = new HashMap<Download, Notification.Builder>();
    }

    @AfterViews
    void processIntentUri() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            if (sharedText != null && !"".equals(sharedText)) {
                downloadAdd(sharedText);
            }

        } else if (Intent.ACTION_VIEW.equals(action)) {
            String uri = intent.getDataString();
            if (uri != null && !"".equals(uri)) {
                downloadAdd(uri);
            }
        }
    }

    @AfterViews
    void loadDownloadsList() {
        try {
            ObjectInputStream io = new ObjectInputStream(openFileInput("downloads.bin"));
            adapter.loadFromStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    //@AfterInject
    //void setupDownloadPrefs() {
        //DownloadItem.setConnectTimeout(prefs.connectTimeout().get());
        //DownloadItem.setReadTimeout(prefs.readTimeout().get());
        //DownloadItem.setDefaultContinue(prefs.continueDownload().get());
    //}


    @Override
    protected void onPause() {
        try {
            ObjectOutputStream io = new ObjectOutputStream(openFileOutput("downloads.bin", MODE_PRIVATE));
            adapter.saveToStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        super.onPause();
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

    void downloadAdd(String uri) {
        DownloadItem downloadItem = new DownloadItem();
        if (uri != null) {
            downloadItem.setUri(uri);
            downloadItem.setFileNameFromUri();
            downloadItem.setFileFolderByExtension();
        }

        AddDownloadFragment addDownloadDialog = new AddDownloadFragment_();
        addDownloadDialog.bind(downloadItem);
        addDownloadDialog.show(getFragmentManager(), "addDownload");
    }

    HashMap<Download,Notification.Builder> downloadNotifications;

    public void downloadChanged(Download proxy) {
        adapter.notifyDataSetChanged();
        getListView().requestFocusFromTouch();

        Downloader downloader = proxy.getDownloader();
        Download.Status status = proxy.getStatus();
        int progress = downloader.getProgressInt();
        String statusName = (
                status == Download.Status.INITIAL? "… ":
                status == Download.Status.STARTED? "▶ ":
                status == Download.Status.PAUSED? "|| ":
                status == Download.Status.FINISHED? "✓ ":
                status == Download.Status.CANCELED? "■ ":
                status == Download.Status.FAILED? "✗ ":
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
              .setOngoing(status == Download.Status.STARTED)
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

        if (status == Download.Status.STARTED || status == Download.Status.PAUSED || status == Download.Status.FAILED) {
            notify.setProgress(100, progress, item.isUnkownSize() && status == Download.Status.STARTED);
        } else {
            notify.setProgress(0, 0, false);
        }

        notifications.notify(item.hashCode(), notify.build());
    }

    public void downloadFailed(Download proxy, Throwable e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @ItemClick
    public void listItemClicked(Download proxy) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(proxy.getItem().getFile()));
        startActivity(intent);
    }
}
