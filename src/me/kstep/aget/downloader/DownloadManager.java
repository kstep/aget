package me.kstep.aget.downloader;

import android.os.Handler;
import com.googlecode.androidannotations.annotations.*;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@EBean
public class DownloadManager extends AbstractList<? extends Downloadable> {
    ThreadPoolExecutor jobsPool;
    List<DownloadProxy> jobs;
    Handler handler = new Handler();
    DownloaderFactory factory = new DownloaderFactory();

    public static interface Listener {
        public void downloadChanged(DownloadProxy proxy);
        public void downloadFailed(DownloadProxy proxy, Throwable e);
    }

    enum Status {
        INITIAL,  // job initialized, nothing done yet
        STARTED,  // job is running
        PAUSED,   // job was paused, restart with resume()
        FINISHED, // job was successfully finished
        FAILED,   // job was failed
        CANCELED, // job was cancelled
    }

    public DownloadManager() {
        jobsPool = new ThreadPoolExecutor(10, 10, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        jobs = new LinkedList<DownloadProxy>();
    }

    public class DownloadProxy implements Runnable, Downloader.Listener {
        private Downloadable item;
        private Future<?> job;
        private Listener listener;

        private Status status;
        public Status getStatus() { return status; }

        DownloadProxy(Downloadable i) {
            item = i;
            status = Status.INITIAL;
            downloader = null;
            job = null;
        }

        public Downloadable getItem() {
            return item;
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
            status = Status.FAILED;
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
                downloader = factory.newDownloader(item.getUri());
                downloader.setListener(this);
            }
            return downloader;
        }

        @Override
        public void run() {

            status = Status.STARTED;
            if (listener != null) {
                listener.downloadChanged(this);
            }

            try {
                downloader = getDownloader();
                downloader.download(item);
                status = Status.FINISHED;

            } catch (InterruptedException e) {

            } catch (RuntimeException e) {
                android.util.Log.e("DownloadProxy", "Error while downloading", e);
                status = Status.FAILED;
                if (listener != null) {
                    listener.downloadFailed(this, e);
                }
            }

            if (listener != null) {
                listener.downloadChanged(this);
            }
        }

        public DownloadProxy setListener(Listener l) {
            listener = l;
            return this;
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
        public DownloadProxy start() {
            startJob();
            return this;
        }

        /**
         * Resume stopped job
         */
        public DownloadProxy resume() {
            getDownloader().setResume(true);
            startJob();
            return this;
        }

        /**
         * Pause started job
         */
        public DownloadProxy pause() {
            stopJob();
            status = Status.PAUSED;
            return true;
        }

        /**
         * Cancel started job
         */
        public DownloadProxy cancel() {
            stopJob();
            status = Status.CANCELED;
            item.getFile().delete();
            return this;
        }

        /**
         * Stop started job (the same as cancel, but do not remove downloaded file)
         */
        public DownloadProxy stop() {
            stopJob();
            status = Status.CANCELED;
            return this;
        }

        /**
         * Restart download job (stop current download, remove file and start download again)
         */
        public DownloadProxy restart() {
            stopJob();
            status = Status.INITIAL;
            item.getFile().delete();
            return start();
        }

        private void updateInfoFromFile() {
            getDownloader().prepare(item.getFile());
            downloadedSize = continueDownload && file.exists()? file.length(): 0;
            status = totalSize > 0 && downloadedSize >= totalSize? Status.FINISHED: Status.INITIAL;
        }
    }

    public DownloadProxy enqueue(Downloadable item) {
        DownloadProxy proxy = this.new DownloadProxy(item);
        jobs.add(proxy);
        return proxy;
    }

    public DownloadProxy enqueue(DownloadProxy proxy) {
        jobs.add(proxy);
        return proxy;
    }

    public void dequeue(DownloadProxy proxy) {
        try { proxy.stop(); } catch (IllegalStateException e) {}
        jobs.remove(proxy);
    }

    @Override
    public boolean add(Downloadable item) {
        enqueue(item);
        return true;
    }

    @Override
    public void clear() {
        for (DownloadProxy proxy : jobs) {
            try {
                proxy.stop();
            } catch (IllegalStateException e) {
            }
        }
        jobs.clear();
    }

    @Override
    public boolean contains(Object object) {
        if (object instanceof DownloadProxy) {
            return jobs.contains(object);
        } else if (object instanceof Downloadable) {
            Uri uri = ((Downloadable) object).getUri();
            for (DownloadProxy proxy : jobs) {
                if (proxy.getItem().getUri().equals(uri)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof DownloadManager) && jobs.equals(((DownloadManager) object).jobs);
    }

    @Override
    public Downloadable get(int pos) {
        return jobs.get(pos).getItem();
    }

    @Override
    public int hashCode() {
        return jobs.hashCode();
    }

    @Override
    public int indexOf(Object object) {
        if (object instanceof DownloadProxy) {
            return jobs.indexOf((DownloadProxy) object);
        } else if (object instanceof Downloadable) {
            int i = 0;
            for (Downloadable job : jobs) {
                if (job.getItem() == object) {
                    return i;
                }
                i++;
            }
            return -1;
        }
    }

    @Override
    public Downloadable remove(int pos) {
        try {
            jobs.remove(pos).stop();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public boolean remove(Downloadable item) {
        if (jobs.remove(item)) {
            try {
                item.stop();
            } catch (IllegalStateException e) {
            }
            return true;
        }
 
        return false;
    }

    @Override
    public boolean isEmpty() {
        return jobs.isEmpty();
    }

    @Override
    public int size() {
        return jobs.size();
    }
}
