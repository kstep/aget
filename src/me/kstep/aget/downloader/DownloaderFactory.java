package me.kstep.aget.downloader;

class DownloadFactory {
    public Downloader newDownloader(Downloadable item) {
        Downloader downloader = null;

        String scheme = item.getUri().getScheme();
        if (scheme.startsWith("http")) {
            downloader = new HttpDownloader();
        }

        if (downloader == null) {
            throw new UnsupportedOperationException("Unsupported URI scheme " + scheme);
        }

        downloader.setResume(true);
        return downloader;
    }
}
