package me.kstep.downloader;

import android.os.Handler;
import com.googlecode.androidannotations.annotations.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@EBean
public class DownloadManager {
    ThreadPoolExecutor jobsPool;
    @Getter List<Download> jobs;
    Handler handler = new Handler();
    DownloaderFactory factory = new DownloaderFactory();

    public DownloadManager() {
        jobsPool = new ThreadPoolExecutor(10, 10, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        jobs = new LinkedList<Download>();
    }

    public class DownloadProxy implements Runnable, Downloader.Listener, Download {
        @Getter private Downloadable item;
        private Future<?> job;
        @Setter private Listener listener;

        @Getter private Download.Status status;

        DownloadProxy(Downloadable i) {
            item = i;
            status = Download.Status.INITIAL;
            downloader = null;
            job = null;
        }

        @Override
        public void downloadChanged(Downloader d) {
            if (listener != null) {
                final DownloadProxy self = this;
                handler.post(new Runnable () {
                    @Override
                    public void run() {
                        listener.downloadChanged(self);
                    }
                });
            }
        }

        @Override
        public void downloadFailed(Downloader d, final Throwable e) {
            status = Download.Status.FAILED;
            if (listener != null) {
                final DownloadProxy self = this;
                handler.post(new Runnable () {
                    @Override
                    public void run() {
                        listener.downloadFailed(self, e);
                    }
                });
            }
        }

        private Downloader downloader;
        public Downloader getDownloader() {
            if (downloader == null) {
                downloader = factory.newDownloader(item);
                downloader.setListener(this);
            }
            return downloader;
        }

        @Override
        public void run() {

            status = Download.Status.STARTED;
            if (listener != null) {
                listener.downloadChanged(this);
            }

            try {
                downloader = getDownloader();
                downloader.download();
                status = Download.Status.FINISHED;

            } catch (InterruptedException e) {

            } catch (RuntimeException e) {
                android.util.Log.e("Download", "Error while downloading", e);
                status = Download.Status.FAILED;
                if (listener != null) {
                    listener.downloadFailed(this, e);
                }
            }

            if (listener != null) {
                listener.downloadChanged(this);
            }
        }

        private boolean isJobRunning() {
            return job != null && !job.isDone() && !job.isCancelled();
        }

        private void startJob() {
            if (isJobRunning()) {
                throw new IllegalStateException("Trying to start an already running download process");
            }
            job = jobsPool.submit(this);
        }

        private void stopJob() {
            if (!isJobRunning()) {
                throw new IllegalStateException("Trying to stop a not running download process");
            }
            job.cancel(false);
        }


        /**
         * Start download
         */
        public void start() {
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
            status = Download.Status.PAUSED;
        }

        /**
         * Cancel started job
         */
        public void cancel() {
            stopJob();
            status = Download.Status.CANCELED;
            item.getFile().delete();
        }

        /**
         * Stop started job (the same as cancel, but do not remove downloaded file)
         */
        public void stop() {
            stopJob();
            status = Download.Status.CANCELED;
        }

        /**
         * Restart download job (stop current download, remove file and start download again)
         */
        public void restart() {
            stopJob();
            status = Download.Status.INITIAL;
            item.getFile().delete();
        }

        private void updateInfoFromFile() {
            getDownloader().prepare(item.getFile());
            downloadedSize = continueDownload && file.exists()? file.length(): 0;
            status = totalSize > 0 && downloadedSize >= totalSize? Download.Status.FINISHED: Download.Status.INITIAL;
        }
    }

    public Download enqueue(Downloadable item) {
        DownloadProxy proxy = this.new DownloadProxy(item);
        jobs.add(proxy);
        return proxy;
    }

    public Download enqueue(Download proxy) {
        jobs.add(proxy);
        return proxy;
    }

    public void dequeue(Download proxy) {
        try { proxy.stop(); } catch (IllegalStateException e) {}
        jobs.remove(proxy);
    }

    public void dequeue(int pos) {
        dequeue(jobs.get(pos));
    }
}
