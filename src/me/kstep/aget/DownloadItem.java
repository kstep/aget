package me.kstep.aget;

import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.Serializable;
import me.kstep.aget.downloader.Downloadable;

class DownloadItem implements Serializable, Downloadable {
    private static final long serialVersionUID = 0L;

    // main meta-data
    private Uri uri = null;
    private String fileName = null;
    private String fileFolder = Environment.DIRECTORY_DOWNLOADS;

    DownloadItem() {}

    DownloadItem(String uri, String fileName) {
        this(Uri.parse(uri), fileName);
    }

    DownloadItem(String uri) {
        this(Uri.parse(uri));
    }

    DownloadItem(Uri uri) {
        this(uri, uri.getLastPathSegment());
    }

    DownloadItem(Uri uri, String fileName) {
        this();
        setUri(uri);
        setFileName(fileName);
    }

    public String getFileName() {
        if (fileName == null || "".equals(fileName)) {
            setFileName();
        }
        return fileName;
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(getFileFolder()), getFileName());
    }

    public DownloadItem setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public DownloadItem setUri(String uri) {
        return setUri(Uri.parse(uri));
    }

    public DownloadItem setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public DownloadItem setFileName() {
        String name = uri.getLastPathSegment();
        return setFileName(name == null || "".equals(name)? "index.html": name);
    }

    @Override
    public String toString() {
        return getFileName();
    }

    public String getFileFolder() {
        return fileFolder;
    }

    public DownloadItem setFileFolder(String folder) {
        this.fileFolder = folder;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Downloadable) && uri.equals(((Downloadable) other).getUri());
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
