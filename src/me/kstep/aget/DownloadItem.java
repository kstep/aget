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

class DownloadItem {

    interface Listener {
        void downloadItemChanged(DownloadItem item);
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

    private URL url;
    private String fileName;
    private long totalSize = -1;
    private long downloadedSize = 0;
    private boolean isRemoteFileName = false;
    private Status status = Status.INITIAL;
    private long lastSpeed = 0;

    final static private long notifyDelay = 2000;
    final static private int bufferSize = 10*1024;
    private boolean continueDownload = true;

    DownloadItem setContinue() {
        return setContinue(true);
    }

    DownloadItem setContinue(boolean value) {
        continueDownload = value;
        return this;
    }

    boolean getContinue() {
        return continueDownload;
    }

    DownloadItem(String url, String fileName) throws MalformedURLException {
        this(new URL(url), fileName);
    }

    DownloadItem(String url) throws MalformedURLException {
        this(new URL(url));
    }

    DownloadItem(URL url) {
        this(url, new File(url.getPath()).getName());
    }

    DownloadItem(URL url, String fileName) {
        this.url = url;
        this.fileName = fileName;
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

    String getFileName() {
        return fileName;
    }

    URL getUrl() {
        return url;
    }

    DownloadItem download(Listener listener) {
        // Declare connection service variables
        HttpURLConnection conn = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        // Reset download status
        status = Status.STARTED;
        downloadedSize = 0;
        lastSpeed = 0;

        // Get local file
        File localFile = getFile();

        try {
            // First we assume we will download from the very beginning
            boolean append = false;
            int success_code = HttpURLConnection.HTTP_OK;

            conn = (HttpURLConnection) openConnection();

            // File exists and we want to continue download, so
            // we make some preparations to download from last place.
            if (continueDownload && localFile.exists()) {
                downloadedSize = localFile.length();
                append = true;
                success_code = HttpURLConnection.HTTP_PARTIAL;
                conn.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }

            // First listener notification.
            long lastNotifyTime = System.currentTimeMillis();
            long lastNotifySize = downloadedSize;
            long time = 0;

            if (listener != null) {
                listener.downloadItemChanged(this);
            }

            conn.connect(); // !!!

            // Make sure server responded OK
            int code = conn.getResponseCode();
            assert code == success_code;

            // Calculate total size
            totalSize = downloadedSize + conn.getContentLength();

            // Initialize streams...
            in = new BufferedInputStream(conn.getInputStream());
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
            android.util.Log.e("aGet", "Download error", e);
            status = Status.FAILED;

        } finally {
            // Cleanup
            if (conn != null) {
                conn.disconnect();
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

        return this;
    }

    // TODO
    DownloadItem setStatus(Status status) {
        this.status = status;
        return this;
    }

    File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getFileName());
    }

    URLConnection openConnection() throws IOException {
        return openConnection("GET");
    }

    URLConnection openConnection(String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(method);
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        return conn;
    }

    DownloadItem setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public String toString() {
        return getFileName();
    }

    String fetchFileName() {
        if (isRemoteFileName) {
            return fileName;
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) openConnection("HEAD");
            conn.connect();

            assert conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            fileName = guessFileNameFromConnection(conn, fileName);
            isRemoteFileName = true;

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
}
