package me.kstep.downloader;

import android.net.Uri;
import java.io.File;
import java.util.HashMap;

public class DownloaderFactory {
    final private static HashMap<String, Class<? extends Downloader>> DOWNLOADERS_MAP = new HashMap<String, Class<? extends Downloader>>();

    public DownloaderFactory() {
        DOWNLOADERS_MAP.put("http", HttpDownloader.class);
        DOWNLOADERS_MAP.put("https", HttpDownloader.class);
    }

    public Downloader newDownloader(Downloadable item) {
        return newDownloader(item.getUri(), item.getFile());
    }

    public Downloader newDownloader(String uri, String file) {
        return getDownloaderByScheme(Uri.parse(uri).getScheme());
    }

    public Downloader newDownloader(Uri uri, File file) {
        return getDownloaderByScheme(uri.getScheme());
    }

    private Downloader getDownloaderByScheme(String scheme) {
        Class<? extends Downloader> downloaderClass = DOWNLOADERS_MAP.get(scheme);

        if (downloaderClass == null) {
            throw new UnsupportedOperationException("Unsupported URI scheme " + scheme);
        }

        Downloader downloader = null;
        try {
            downloader = downloaderClass.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }

        if (downloader == null) {
            throw new UnsupportedOperationException("Unsupported URI scheme " + scheme);
        }

        //downloader.setUri(item.getUri());
        //downloader.setFile(item.getFile());
        downloader.setResume(true);
        return downloader;
    }
}
