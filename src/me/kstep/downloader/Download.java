package me.kstep.downloader;

public class Download implements Runnable, Downloader.Listener {

    public Download(Downloadable i) {
        item = i;
        status = Status.INITIAL;
        job = null;
        downloader = null;
    }

    // Inner classes {{{
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
    // }}}

    // Fields, setters and getters {{{
    @Getter private Downloadable item;
    @Getter private Status status;
    private Future<?> job;

    private Downloader downloader;
    public Downloader getDownloader() {
        if (downloader == null) {
            downloader = getDownloaderFactory().newDownloader(item);
            downloader.setListener(this);
        }
        return downloader;
    }
    // }}}

    // Static factories, pools etc. {{{
    private static ThreadPoolExecutor jobsPool = null;
    public static ThreadPoolExecutor getJobsPool() {
        if (jobsPool == null) {
            jobsPool = new ThreadPoolExecutor(10, 10, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
        return jobsPool;
    }

    public static void setJobsPool(ThreadPoolExecutor value) {
        jobsPool = value;
    }

    private static DownloaderFactory downloaderFactory = null;
    public static void setDownloaderFactory(DownloaderFactory factory) {
        downloaderFactory = factory;
    }

    public static DownloaderFactory getDownloaderFactory() {
        if (downloaderFactory == null) {
            downloaderFactory = new DownloaderFactory();
        }
        return downloaderFactory;
    }
    // }}}

    // Downloader.Listener interface {{{
    @Override
    public void downloadStarted(Downloader d) {
        status = Status.STARTED;
        notifyListener();
    }

    @Override
    public void downloadEnded(Downloader d) {
        notifyListener();
    }

    @Override
    public void downloadProgress(Downloader d) {
        notifyListener();
    }

    @Override
    public void downloadFinished(Downloader d) {
        status = Status.FINISHED;
        notifyListener();
    }

    @Override
    public void downloadStopped(Downloader d) {
        //status = Status.PAUSED;
        notifyListener();
    }

    @Override
    public void downloadFailed(Downloader d, final Throwable e) {
        status = Status.FAILED;
        if (listener != null) {
            final Download self = this;
            handler.post(new Runnable () {
                @Override
                public void run() {
                    listener.downloadFailed(self, e);
                }
            });
        }
    }

    private void notifyListener() {
        if (listener != null) {
            final Download self = this;
            handler.post(new Runnable () {
                @Override
                public void run() {
                    listener.downloadChanged(self);
                }
            });
        }
    }
    // }}}

    // Basic thread control {{{
    private boolean isJobRunning() {
        return job != null && !job.isDone() && !job.isCancelled();
    }

    private void startJob() {
        if (isJobRunning()) {
            throw new IllegalStateException("Trying to start an already running download process");
        }

        Downloader downloader = getDownloader();
        downloader.setUri(item.getUri());
        downloader.setFile(item.getFile());

        job = getJobsPool().submit(downloader);
    }

    private void stopJob() {
        if (!isJobRunning()) {
            throw new IllegalStateException("Trying to stop a not running download process");
        }

        job.cancel(true);
    }
    // }}}

    // Public download control interface {{{
    /**
     * Start download
     */
    public void start() {
        item.getFile().getParentFile().mkdirs();
        startJob();
    }

    /**
     * Resume stopped job
     */
    public void resume() {
        getDownloader().setResume(true);
        startJob();
    }

    /**
     * Pause started job
     */
    public void pause() {
        stopJob();
        status = Status.PAUSED;
    }

    /**
     * Cancel started job
     */
    public void cancel() {
        stopJob();
        status = Status.CANCELED;
        item.getFile().delete();
    }

    /**
     * Stop started job (the same as cancel, but do not remove downloaded file)
     */
    public void stop() {
        stopJob();
        status = Status.CANCELED;
    }

    /**
     * Restart download job (stop current download, remove file and start download again)
     */
    public void restart() {
        stopJob();
        status = Status.INITIAL;
        item.getFile().delete();
    }
    // }}}
}
