package me.kstep.downloader;

public interface Download {
    public Downloader getDownloader();
    public Downloadable getItem();
    public Status getStatus();

    public void start();
    public void stop();
    public void pause();
    public void resume();
    public void restart();
    public void cancel();

    public enum Status {
        INITIAL,  // job initialized, nothing done yet
        STARTED,  // job is running
        PAUSED,   // job was paused, restart with resume()
        FINISHED, // job was successfully finished
        FAILED,   // job was failed
        CANCELED, // job was cancelled
    }

    public static interface Listener {
        public void downloadChanged(Download proxy);
        public void downloadFailed(Download proxy, Throwable e);
    }
}
