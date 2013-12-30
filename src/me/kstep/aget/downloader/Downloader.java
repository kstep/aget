package me.kstep.aget.downloader;

import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class Downloader {
    public static class FileMetaInfo {
        public String mimeType = null;
        public String fileName = null;
        public long totalSize = -1;
    }

    final static private int bufferSize = 10*1024;
    final static protected int connectTimeout = 2000;
    final static protected int readTimeout = 5000;

    protected long totalSize;
    public long getTotalSize() {
        return totalSize;
    }
    public boolean isUnknownSize() {
        return totalSize < 0;
    }

    protected long downloadedSize;
    public long getDownloadedSize() {
        return downloadedSize;
    }

    protected boolean resume;
    public boolean getResume() {
        return resume;
    }
    public Downloader setResume(boolean value) {
        resume = value;
        return this;
    }

    public static interface Listener {
        public void downloadChanged(Downloader downloader);
        public void downloadFailed(Downloader downloader, Throwable e);
    }
    protected Listener listener;
    public Downloader setListener(Listener l) {
        listener = l;
        return this;
    }

    Downloader() {
        totalSize = -1;
        downloadedSize = 0;
        lastSpeed = 0;

        lastNotifyTime = 0;
        lastNotifiedDownloadedSize = 0;
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

    private long lastSpeed;
    private long lastNotifyTime = 0;
    private long lastNotifiedDownloadedSize = 0;
    final static private long notifyDelay = 2000;

    public long getLastSpeed() {
        return lastSpeed;
    }

    public long getTimeLeft() {
        return lastSpeed > 0? (totalSize - downloadedSize) / lastSpeed: -1;
    }

    /**
     * Called on connection initialization, must open connection to the URI,
     * initialize it according settings (timeouts and resume settings)
     */
    protected abstract InputStream openConnection(Uri uri, File file) throws IOException;
    protected abstract void closeConnection();

    final public void download(Downloadable item) {
        download(item.getUri(), item.getFile());
    }

    public abstract FileMetaInfo getMetaInfo(Uri uri);

    final public FileMetaInfo getMetaInfo(Downloadable item) {
        return getMetaInfo(item.getUri());
    }

    final public void download(Uri uri, File file) {
        downloadedSize = 0;

        if (listener != null) {
            listener.downloadChanged(this);
        }

        // Reset connection service variables
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            // Initialize streams...
            in = new BufferedInputStream(openConnection(uri, file));
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
                            listener.downloadChanged(this);
                        }
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }

        } catch (IOException e) {
            android.util.Log.e("Downloader", "Download error", e);
            if (listener != null) {
                listener.downloadFailed(this, e);
            }

        } catch (InterruptedException e) {

        } finally {
            closeConnection();
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
            listener.downloadChanged(this);
        }
    }
}
