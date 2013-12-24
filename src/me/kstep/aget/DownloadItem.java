package me.kstep.aget;

import android.content.Context;
import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import com.googlecode.androidannotations.annotations.*;

@EBean
class DownloadItem {

    interface Listener {
        void downloadItemChanged(DownloadItem item);
        void downloadItemFailed(DownloadItem item, Throwable e);
    }

    // INITIAL -> STARTED
    // STARTED -> PAUSED|FINISHED|CANCELED|FAILED
    // FAILED -> INITIAL|STARTED|CANCELED
    // CANCELED -> INITIAL|STARTED
    // FINISHED -> CANCELED|INITIAL
    enum Status {
        INITIAL,
        STARTED,
        PAUSED,
        FINISHED,
        FAILED,
        CANCELED,
    }

    private URL url = null;
    private String fileName;
    private long totalSize = -1;
    private long downloadedSize = 0;
    private Status status = Status.INITIAL;
    private long lastSpeed = 0;
    private HttpURLConnection connection = null;

    final static private long notifyDelay = 2000;
    final static private int bufferSize = 10*1024;
    private boolean continueDownload = true;

    public static DownloadItem fromUrl(String url, String fileName) throws MalformedURLException {
        return fromUrl(new URL(url), fileName);
    }

    public static DownloadItem fromUrl(String url) throws MalformedURLException {
        return fromUrl(new URL(url));
    }

    public static DownloadItem fromUrl(URL url) {
        return fromUrl(url, new File(url.getPath()).getName());
    }

    public static DownloadItem fromUrl(URL url, String fileName) {
        DownloadItem item = new DownloadItem();
        item.setUrl(url);
        item.setFileName(fileName);
        return item;
    }

    public Status getStatus() {
        return status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public long getLastSpeed() {
        return lastSpeed;
    }

    public String getFileName() {
        return fileName;
    }

    public URL getUrl() {
        return url;
    }

    public DownloadItem setContinue() {
        return setContinue(true);
    }

    public DownloadItem setContinue(boolean value) {
        continueDownload = value;
        return this;
    }

    public boolean getContinue() {
        return continueDownload;
    }

    public DownloadItem setUrl(URL url) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }
        this.url = url;
        return this;
    }

    public DownloadItem setUrl(String url) throws MalformedURLException {
        return setUrl(new URL(url));
    }

    @Background
    void download(Listener listener) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }

        // Reset download status
        status = Status.STARTED;
        downloadedSize = 0;
        lastSpeed = 0;

        // Declare connection service variables
        connection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        // Get local file
        File localFile = getFile();

        try {
            // First we assume we will download from the very beginning
            boolean append = false;
            int success_code = HttpURLConnection.HTTP_OK;

            connection = (HttpURLConnection) openConnection();

            // File exists and we want to continue download, so
            // we make some preparations to download from last place.
            if (continueDownload && localFile.exists()) {
                downloadedSize = localFile.length();
                append = true;
                success_code = HttpURLConnection.HTTP_PARTIAL;
                connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }

            // First listener notification.
            long lastNotifyTime = System.currentTimeMillis();
            long lastNotifySize = downloadedSize;
            long time = 0;

            if (listener != null) {
                listener.downloadItemChanged(this);
            }

            connection.connect(); // !!!

            // Make sure server responded OK
            int code = connection.getResponseCode();
            assert code == success_code;

            // Calculate total size
            totalSize = downloadedSize + connection.getContentLength();

            // Initialize streams...
            in = new BufferedInputStream(connection.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(localFile, append));

            // ...and buffers
            byte[] buffer = new byte[bufferSize];
            int readBytes = 0;

            // Main download loop
            while ((readBytes = in.read(buffer)) >= 0) {
                if (readBytes > 0) {
                    out.write(buffer, 0, readBytes);
                    downloadedSize += readBytes;

                    // Notify listener from time to time, ...
                    if (listener != null) {
                        time = System.currentTimeMillis();
                        // ...but not too often!
                        if ((time - lastNotifyTime) > notifyDelay) {
                            lastSpeed = (downloadedSize - lastNotifySize) / ((time - lastNotifyTime) / 1000);
                            lastNotifyTime = time;
                            lastNotifySize = downloadedSize;
                            listener.downloadItemChanged(this);
                        }
                    }
                }
            }

            // We are done!
            status = Status.FINISHED;

        } catch (IOException e) {
            if (status != Status.CANCELED) {
                android.util.Log.e("aGet", "Download error", e);
                status = Status.FAILED;
                if (listener != null) {
                    listener.downloadItemFailed(this, e);
                }
            }

        } finally {
            // Cleanup
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
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

        // Last listener notification.
        if (listener != null) {
            listener.downloadItemChanged(this);
        }
    }

    public File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getFileName());
    }

    private URLConnection openConnection() throws IOException {
        return openConnection("GET");
    }

    private URLConnection openConnection(String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(method);
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        return conn;
    }

    public DownloadItem setFileName(String fileName) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }
        this.fileName = fileName;
        return this;
    }

    @Override
    public String toString() {
        return getFileName();
    }

    String fetchFileName() {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) openConnection("HEAD");
            conn.connect();

            assert conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            fileName = guessFileNameFromConnection(conn, fileName);

            if (totalSize == -1) {
                totalSize = conn.getContentLength();
            }

        } catch (IOException e) {
            android.util.Log.w("DownloadItem", "ERROR", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return fileName;
    }

    private String guessFileNameFromConnection(URLConnection conn, String fileName) {
        String header = conn.getHeaderField("Content-Disposition");
        if (header == null) {
            header = conn.getHeaderField("Content-Type");
        }

        if (header != null) {
            fileName = getFileNameFromHeader(header, fileName);
        }

        return fileName;
    }

    private String getFileNameFromHeader(String header, String def) {
        int p = header.indexOf("name=");
        if (p < 0) return def;

        header = header.substring(p + 5);
        if (header.charAt(0) == '"') {
            header = header.substring(1, header.length() - 1);
        }

        return header;
    }

    public DownloadItem cancelDownload(boolean deleteLocalFile) {
        status = Status.CANCELED;
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        if (deleteLocalFile) {
            getFile().delete();
        }
        return this;
    }

    public DownloadItem cancelDownload() {
        return cancelDownload(false);
    }

    public DownloadItem startDownload(Listener listener) {
        download(listener);
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof DownloadItem) && url.equals(((DownloadItem) other).getUrl());
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
