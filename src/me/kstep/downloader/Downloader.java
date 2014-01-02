package me.kstep.downloader;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import lombok.Setter;

public abstract class Downloader implements Runnable {
    public static class FileMetaInfo {
        public String mimeType = null;
        public String fileName = null;
        public long totalSize = -1;
    }

    @Setter static private ConnectivityManager connectivity;

    @Getter @Setter private Uri uri;
    @Getter @Setter private File file;

    @Setter static private int bufferSize = 10*1024;
    @Setter static protected int connectTimeout = 2000;
    @Setter static protected int readTimeout = 5000;
    @Setter static private boolean defaultResume = true;
    @Setter static private boolean defaultInsecure = true;
    @Setter static private boolean useWiFiOnly = false;

    @Getter protected long totalSize;
    public boolean isUnknownSize() {
        return totalSize < 0;
    }

    @Getter protected long downloadedSize;

    @Getter @Setter protected boolean resume;
    @Getter @Setter protected boolean insecure;

    public static interface Listener {
        public void downloadStarted(Downloader downloader); // started download
        public void downloadProgress(Downloader downloader); // progress changed
        public void downloadFinished(Downloader downloader); // successfully finished
        public void downloadStopped(Downloader downloader); // force stopped
        public void downloadFailed(Downloader downloader, Throwable e); // failed download
        public void downloadEnded(Downloader downloader); // ended download
    }
    @Setter protected Listener listener;

    Downloader() {
        totalSize = -1;
        downloadedSize = 0;
        lastSpeed = 0;

        lastNotifyTime = 0;
        lastNotifiedDownloadedSize = 0;

        resume = defaultResume;
        insecure = defaultInsecure;
    }

    Downloader(Uri uri, File file) {
        this();
        this.uri = uri;
        this.file = file;
    }

    Downloader(Uri uri) {
        this();
        this.uri = uri;
    }

    Downloader(File file) {
        this();
        this.file = file;
    }

    public FileMetaInfo init() {
        FileMetaInfo meta = getMetaInfo(uri, file);
        this.totalSize = meta.totalSize;
        this.downloadedSize = resume && file.exists()? file.length(): 0;
        this.lastSpeed = 0;
        return meta;
    }

    public int getProgressInt() {
        return totalSize > 0? (int) (downloadedSize * 100 / totalSize): -1;
    }

    public float getProgress() {
        return totalSize > 0? (float) (downloadedSize * 100.0 / totalSize): -1;
    }

    public long getLeftSize() {
        return totalSize - downloadedSize;
    }

    @Getter private long lastSpeed;
    private long lastNotifyTime = 0;
    private long lastNotifiedDownloadedSize = 0;
    final static private long notifyDelay = 2000;

    public long getTimeLeft() {
        return lastSpeed > 0? (totalSize - downloadedSize) / lastSpeed: -1;
    }

    /**
     * Called on connection initialization, must open connection to the URI,
     * initialize it according settings (timeouts and resume settings)
     */
    protected abstract InputStream openConnection(Uri uri, File file) throws IOException;
    protected abstract void closeConnection(InputStream is);
    public abstract FileMetaInfo getMetaInfo(Uri uri, File file);

    final public void run() {
        downloadedSize = 0;

        if (listener != null) {
            listener.downloadStarted(this);
        }

        // Reset connection service variables
        InputStream is = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {

            NetworkInfo network = connectivity.getActiveNetworkInfo();
            if (network == null || network.isConnected()) {
                throw new IOException("No active network connection found");
            }

            if (useWiFiOnly) {
                switch (network.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_ETHERNET:
                    break;
                default:
                    throw new IOException("Non-WiFi network usage prohibited");
                }
            }

            // Initialize streams...
            in = new BufferedInputStream(is = openConnection(uri, file));
            out = new BufferedOutputStream(new FileOutputStream(file, resume));

            // ...and buffers
            byte[] buffer = new byte[bufferSize];
            int readBytes = 0;

            // Main download loop
            while ((readBytes = in.read(buffer)) >= 0) {
                if (readBytes > 0) {
                    out.write(buffer, 0, readBytes);
                    downloadedSize += readBytes;

                    long time = System.currentTimeMillis();
                    if ((time - lastNotifyTime) >= notifyDelay) {
                        lastSpeed = (downloadedSize - lastNotifiedDownloadedSize) / ((time - lastNotifyTime) / 1000);
                        lastNotifyTime = time;
                        lastNotifiedDownloadedSize = downloadedSize;

                        if (listener != null) {
                            listener.downloadProgress(this);
                        }
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }

            if (listener != null) {
                listener.downloadFinished(this);
            }

        } catch (IOException e) {
            android.util.Log.e("Downloader", "Download error", e);
            if (listener != null) {
                listener.downloadFailed(this, e);
            }

        } catch (InterruptedException e) {
            if (listener != null) {
                listener.downloadStopped(this);
            }

        } finally {
            closeConnection(is);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }

        if (listener != null) {
            listener.downloadEnded(this);
        }
    }
}
