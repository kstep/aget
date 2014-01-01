package me.kstep.aget;

import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.Serializable;
import me.kstep.downloader.Downloadable;

class DownloadItem implements Serializable, Downloadable {
    private static final long serialVersionUID = 0L;

    // main meta-data
    @Getter @Setter private Uri uri = null;
    @Getter @Setter private String fileName = null;
    @Getter @Setter private String fileFolder = Environment.DIRECTORY_DOWNLOADS;

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

    @Override
    public File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(getFileFolder()), getFileName());
    }

    public void setUri(String uri) {
        setUri(Uri.parse(uri));
    }

    public void setFileName() {
        String name = uri.getLastPathSegment();
        setFileName(name == null || "".equals(name)? "index.html": name);
    }

    @Override
    public String toString() {
        return getFileName();
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
