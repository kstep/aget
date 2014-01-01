package me.kstep.downloader;

class DownloaderFactory {
    public Downloader newDownloader(Downloadable item) {
        Downloader downloader = null;

        String scheme = item.getUri().getScheme();
        if (scheme.startsWith("http")) {
            downloader = new HttpDownloader();
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
