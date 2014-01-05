package me.kstep.aget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import com.googlecode.androidannotations.annotations.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloader;

@EService
public class DownloadManagerService extends Service
    implements Download.Listener {

    @SystemService
    NotificationManager notifications;

    private HashMap<Download,Notification.Builder> downloadNotifications;

    @Override
    public void downloadChanged(Download proxy) {
        for (Download.Listener subscriber : subscribers) {
            subscriber.downloadChanged(proxy);
        }

        Downloader downloader = proxy.getDownloader();
        DownloadItem item = (DownloadItem) proxy.getItem();

        int status = proxy.getStatus();
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
                new Intent(this, DownloadManagerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT
                );
        if (downloadNotifications.containsKey(proxy)) {
            notify = downloadNotifications.get(proxy);
        } else {
            notify = new Notification.Builder(this)
              .setSmallIcon(R.drawable.ic_launcher)
              .setContentIntent(pi)
              //.addAction(R.drawable.ic_action_settings, "Open", pi)
              //.addAction(R.drawable.ic_action_cancel, "Cancel", pi)
              ;
            downloadNotifications.put(proxy, notify);
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
                          Util.humanizeTime(downloader.getTimeLeft()),
                          progress
                          ))
              .setContentText(String.format("%s/%s @ %s/s",
                          Util.humanizeSize(downloader.getDownloadedSize()),
                          Util.humanizeSize(downloader.getTotalSize()),
                          Util.humanizeSize(downloader.getLastSpeed())
                          ))
              ;

        if (status == Download.Status.STARTED || status == Download.Status.PAUSED || status == Download.Status.FAILED) {
            notify.setProgress(100, progress, downloader.isUnknownSize() && status == Download.Status.STARTED);
        } else {
            notify.setProgress(0, 0, false);
        }

        notifications.notify(item.hashCode(), notify.build());
    }

    @Override
    public void downloadFailed(Download proxy, Throwable e) {
        for (Download.Listener subscriber : subscribers) {
            subscriber.downloadFailed(proxy, e);
        }

        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @SystemService
    NotificationManager notificationManager;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        DownloadManagerService getService() {
            return DownloadManagerService.this;
        }
    }

    @Override
    public void onCreate() {
        items = new LinkedList<Download>();
        downloadNotifications = new HashMap<Download, Notification.Builder>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri uri = intent.getData();

        if (uri != null) {
            Bundle extras = intent.getExtras();

            DownloadItem item = new DownloadItem(intent.getData());
            item.setFileName(extras.getString("fileName"));
            item.setFileFolder(extras.getString("fileFolder"));

            Download download = new Download(item);
            Downloader downloader = download.getDownloader();
            downloader.setInsecure(extras.getBoolean("insecure"));
            downloader.setResume(extras.getBoolean("resume"));

            download.setListener(this);

            items.add(download);
            if (extras.getBoolean("start")) {
                download.start();
            }
        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private List<Download> items;
    public List<Download> getDownloadList() {
        return items;
    }

    List<Download.Listener> subscribers = new LinkedList<Download.Listener>();
    public void subscribe(Download.Listener subscriber) {
        subscribers.add(subscriber);
    }
}
