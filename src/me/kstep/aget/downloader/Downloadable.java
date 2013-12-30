package me.kstep.aget.downloader;

import android.net.Uri;
import java.io.File;

public interface Downloadable {
    public Uri getUri();
    public File getFile();
}
