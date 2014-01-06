package me.kstep.downloader;

import android.net.Uri;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;

public class Download implements Downloader.Listener, Parcelable {

    public Download(Downloadable i) {
        item = i;
        status = Status.INITIAL;
        job = null;
        downloader = null;
    }

    public Download(Parcel in) {
        downloader = in.readParcelable(Downloader.class.getClassLoader());

        final File file = new File(in.readString());
        final Uri uri = Uri.parse(in.readString());

        item = new Downloadable () {
            public File getFile() { return file; }
            public Uri getUri() { return uri; }
        };

        //status = in.readInt("status");
    }

    // Inner classes {{{
    final public static class Status {
        final public static int INITIAL = 0;  // job initialized, nothing done yet
        final public static int STARTED = 1;  // job is running
        final public static int PAUSED = 2;   // job was paused, restart with resume()
        final public static int FINISHED = 3; // job was successfully finished
        final public static int FAILED = 4;   // job was failed
        final public static int CANCELED = 5; // job was cancelled
    }

    public static interface Listener {
        public void downloadChanged(Download proxy);
        public void downloadFailed(Download proxy, Throwable e);
    }
    // }}}

    // Fields, setters and getters {{{
    final private Handler handler = new Handler();

    @Setter private Listener listener;
    @Getter private Downloadable item;
    @Getter private int status;
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
            handler.post(new Runnable () {
                @Override
                public void run() {
                    listener.downloadFailed(Download.this, e);
                }
            });
        }
    }

    private void notifyListener() {
        if (listener != null) {
            handler.post(new Runnable () {
                @Override
                public void run() {
                    listener.downloadChanged(Download.this);
                }
            });
        }
    }
    // }}}

    // Basic thread control {{{
    private boolean isJobRunning() {
        return job != null && !job.isDone() && !job.isCancelled();
    }

    private boolean startJob() {
        if (isJobRunning()) {
            return false;
            //throw new IllegalStateException("Trying to start an already running download process");
        }

        Downloader downloader = getDownloader();
        downloader.setUri(item.getUri());
        downloader.setFile(item.getFile());

        job = getJobsPool().submit(downloader);
        return true;
    }

    private boolean stopJob() {
        if (!isJobRunning()) {
            return false;
            //throw new IllegalStateException("Trying to stop a not running download process");
        }

        job.cancel(true);
        return true;
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

    // Parcelable inteface {{{
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(getDownloader(), 0);
        out.writeString(getItem().getFile().toString());
        out.writeString(getItem().getUri().toString());
        //out.writeInt(status);
    }

    final public static Parcelable.Creator<Download> CREATOR = new Parcelable.Creator<Download> () {
        @Override
        public Download createFromParcel(Parcel in) {
            return new Download(in);
        }

        @Override
        public Download[] newArray(int size) {
            return new Download[size];
        }
    };
    // }}}
}
