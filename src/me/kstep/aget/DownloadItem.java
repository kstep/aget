package me.kstep.aget;

import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import java.io.File;
import lombok.Getter;
import lombok.Setter;
import me.kstep.downloader.Downloadable;

class DownloadItem implements Downloadable {

    // main meta-data
    @Getter private Uri uri = null;
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

    public String getMimeTypeByExtension() {
        String mimeType = MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(fileName));
        return mimeType == null? "application/octet-stream": mimeType;
    }

    public void setUri(String uri) {
        setUri(Uri.parse(uri));
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setFileNameFromUri() {
        String name = uri.getLastPathSegment();
        setFileName(name == null || "".equals(name)? "index.html": name);
    }

    public void setFileFolderByExtension() {
        setFileFolderByMimeType(getMimeTypeByExtension());
    }

    public void setFileFolderByMimeType(String mimeType) {
        if (mimeType == null) {
            mimeType = getMimeTypeByExtension();
        }

        setFileFolder(mimeType.startsWith("video/")? Environment.DIRECTORY_MOVIES:
                      mimeType.startsWith("audio/")? Environment.DIRECTORY_MUSIC:
                      mimeType.startsWith("image/")? Environment.DIRECTORY_PICTURES:
                      Environment.DIRECTORY_DOWNLOADS);
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
