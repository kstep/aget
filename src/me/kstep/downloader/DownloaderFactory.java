package me.kstep.downloader;

import java.util.HashMap;

class DownloaderFactory {
    final private static HashMap<String, Class<? extends Downloader>> DOWNLOADERS_MAP = new HashMap<String, Class<? extends Downloader>>();

    DownloaderFactory() {
        DOWNLOADERS_MAP.put("http", HttpDownloader.class);
        DOWNLOADERS_MAP.put("https", HttpDownloader.class);
    }

    public Downloader newDownloader(Downloadable item) {
        String scheme = item.getUri().getScheme();
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
