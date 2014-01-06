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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Iterator;
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
        notifySubscribers(proxy);

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
        for (WeakReference<Download.Listener> subscriber : subscribers) {
	    try {
                subscriber.get().downloadFailed(proxy, e);
	    } catch (NullPointerException _) {
	    }
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

    private static class SerializableDownloadItem implements Serializable {
        private static final long serialVersionUID = 0L;

        String uri;
        String folder;
        String file;

        SerializableDownloadItem(String uri, String folder, String file) {
            this.uri = uri;
            this.folder = folder;
            this.file = file;
        }

        SerializableDownloadItem(Uri uri, String folder, String file) {
            this(uri.toString(), folder, file);
        }

        SerializableDownloadItem(DownloadItem item) {
            this(item.getUri(), item.getFileFolder(), item.getFileName());
        }

        Download getDownload() {
            DownloadItem item = new DownloadItem(uri, file);
            item.setFileFolder(folder);
            return new Download(item);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate() {
        super.onCreate();

        items = new LinkedList<Download>();

        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(openFileInput("downloads.bin"));
            while (true) {
                items.add(((SerializableDownloadItem) is.readObject()).getDownload());
            }

        } catch (OptionalDataException e) {
        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) {}
            }
        }

        downloadNotifications = new HashMap<Download, Notification.Builder>();
    }

    @Override
    public void onDestroy() {
        for (Download item : items) {
            item.stop();
        }

        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(openFileOutput("downloads.bin", MODE_PRIVATE));
            for (Download item : items) {
                os.writeObject(new SerializableDownloadItem((DownloadItem) item.getItem()));
            }

        } catch (IOException e) {
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException e) {}
            }
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri uri = intent.getData();

        if (uri != null) {
            Bundle extras = intent.getExtras();

	    try {
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

		notifySubscribers(download);

	    } catch (UnsupportedOperationException e) {
                Toast.makeText(this, R.string.error_bad_uri, Toast.LENGTH_LONG).show();
	    }
        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private List<Download> items = null;
    public List<Download> getDownloadList() {
        return items;
    }

    List<WeakReference<Download.Listener>> subscribers = new LinkedList<WeakReference<Download.Listener>>();
    public void subscribe(Download.Listener subscriber) {
        subscribers.add(new WeakReference<Download.Listener>(subscriber));
    }
    
    private void notifySubscribers(Download download) {
	Iterator<WeakReference<Download.Listener>> iter = subscribers.iterator();
	int removed = 0;

	while (iter.hasNext()) {
	    Download.Listener subscriber = iter.next().get();
	    if (subscriber == null) {
		iter.remove();
		removed++;
	    } else {
		subscriber.downloadChanged(download);
	    }
	}

	if (removed > 0) {
	    System.gc();
	}
    }
}
