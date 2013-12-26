package me.kstep.aget;

import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class DownloadItem implements Serializable {
    private static final long serialVersionUID = 0L;

    interface Listener {
        void downloadItemChanged(DownloadItem item);
        void downloadItemFailed(DownloadItem item, Throwable e);
    }

    // INITIAL -> STARTED
    // STARTED -> PAUSED|FINISHED|CANCELED|FAILED
    // FAILED -> INITIAL|STARTED|CANCELED
    // CANCELED -> INITIAL|STARTED
    // FINISHED -> CANCELED|INITIAL
    enum Status {
        INITIAL,
        STARTED,
        PAUSED,
        FINISHED,
        FAILED,
        CANCELED,
    }

    // main meta-data
    private URL url = null;
    private String fileName = null;
    private String fileFolder = Environment.DIRECTORY_DOWNLOADS;

    // server provided data
    private long totalSize = -1;
    private String mimeType = null;

    // download bookkeeping data
    private Thread downloadThread = null;
    private Status status = Status.INITIAL;
    private long downloadedSize = 0;
    private long lastSpeed = 0;

    // common download configuration settings
    final static private long notifyDelay = 2000;

    static private int bufferSize = 10*1024;
    static private int readTimeout = 5000;
    static private int connectTimeout = 2000;
    static private boolean defaultContinueDownload = true;

    public static void setReadTimeout(int value) {
        readTimeout = value;
    }
    public static void setConnectTimeout(int value) {
        connectTimeout = value;
    }
    public static void setBufferSize(int value) {
        bufferSize = value;
    }
    public static void setDefaultContinue(boolean value) {
        defaultContinueDownload = value;
    }


    // instance specific configuration settings
    private boolean continueDownload = true;
    private boolean ignoreCertificate = true;

    DownloadItem() {
        continueDownload = defaultContinueDownload;
    }

    DownloadItem(String url, String fileName) throws MalformedURLException {
        this(new URL(url), fileName);
    }

    DownloadItem(String url) throws MalformedURLException {
        this(new URL(url));
    }

    DownloadItem(URL url) {
        this(url, new File(url.getPath()).getName());
    }

    DownloadItem(URL url, String fileName) {
        this();
        setUrl(url);
        setFileName(fileName);
    }

    public Status getStatus() {
        return status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public long getLastSpeed() {
        return lastSpeed;
    }

    public String getFileName() {
        if (fileName == null || "".equals(fileName)) {
            setFileName();
        }
        return fileName;
    }

    public URL getUrl() {
        return url;
    }

    public DownloadItem setContinue() {
        return setContinue(true);
    }

    public DownloadItem setContinue(boolean value) {
        continueDownload = value;
        return this;
    }

    public boolean isContinue() {
        return continueDownload;
    }

    public DownloadItem setUrl(URL url) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }
        this.url = url;
        return this;
    }

    public DownloadItem setUrl(String url) throws MalformedURLException {
        return setUrl(new URL(url));
    }

    void download(Listener listener) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }

        // Reset download status
        status = Status.STARTED;
        downloadedSize = 0;
        lastSpeed = 0;

        // Reset connection service variables
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        // Get local file
        File localFile = getFile();

        try {
            // First we assume we will download from the very beginning
            boolean append = false;
            int success_code = HttpURLConnection.HTTP_OK;

            connection = openConnection();

            // File exists and we want to continue download, so
            // we make some preparations to download from last place.
            if (isContinue() && localFile.exists()) {
                append = true;
                success_code = HttpURLConnection.HTTP_PARTIAL;
                downloadedSize = localFile.length();
                connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }

            // First listener notification.
            long lastNotifyTime = System.currentTimeMillis();
            long lastNotifySize = downloadedSize;
            long time = 0;

            if (listener != null) {
                listener.downloadItemChanged(this);
            }

            connect(connection, success_code); // !!! Now network connection is available !!!

            // Calculate total size
            totalSize = getContentLengthFromConnection(connection);
            if (totalSize >= 0) {
                totalSize += downloadedSize;
            }

            // Update MIME-type if not done before with fetchMetaData()
            if (mimeType == null) {
                mimeType = guessMimeTypeFromConnectionAndFileName(connection, fileName, "application/octet-stream", false);
            }

            // Initialize streams...
            in = new BufferedInputStream(connection.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(localFile, append));

            // ...and buffers
            byte[] buffer = new byte[bufferSize];
            int readBytes = 0;

            // Main download loop
            while ((readBytes = in.read(buffer)) >= 0) {
                if (readBytes > 0) {
                    out.write(buffer, 0, readBytes);
                    downloadedSize += readBytes;

                    // Notify listener from time to time, ...
                    if (listener != null) {
                        time = System.currentTimeMillis();
                        // ...but not too often!
                        if ((time - lastNotifyTime) > notifyDelay) {
                            lastSpeed = (downloadedSize - lastNotifySize) / ((time - lastNotifyTime) / 1000);
                            lastNotifyTime = time;
                            lastNotifySize = downloadedSize;
                            listener.downloadItemChanged(this);
                        }
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }

            // We are done!
            status = Status.FINISHED;

        } catch (IOException e) {
            if (status == Status.STARTED) {
                android.util.Log.e("aGet", "Download error", e);
                status = Status.FAILED;
                if (listener != null) {
                    listener.downloadItemFailed(this, e);
                }
            }

        } catch (InterruptedException e) {

        } finally {
            // Cleanup
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }

        // Last listener notification.
        if (listener != null) {
            listener.downloadItemChanged(this);
        }
    }

    public File getFile() {
        return new File(Environment.getExternalStoragePublicDirectory(getFileFolder()), getFileName());
    }

    private HttpURLConnection openConnection() throws IOException {
        return openConnection("GET");
    }

    private HttpURLConnection connect(HttpURLConnection conn, int expectedCode) throws IOException {
        conn.connect();
        int code = conn.getResponseCode();
        if (code != expectedCode) {
            throw new IOException(String.format("Invalid return code %d, expected %d", code, expectedCode));
        }
        return conn;
    }

    private HttpURLConnection connect(HttpURLConnection conn) throws IOException {
        return connect(conn, HttpURLConnection.HTTP_OK);
    }

    private HttpURLConnection openConnection(String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(method);
		conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
		
		if (conn instanceof HttpsURLConnection && ignoreCertificate) {
		    ((HttpsURLConnection) conn).setSSLSocketFactory(getTrustAllSocketFactory());
		}

        return conn;
    }
	
	private SSLSocketFactory trustAllSocketFactory = null;
	private SSLSocketFactory getTrustAllSocketFactory() {
		if (trustAllSocketFactory == null) {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// Not implemented
					}

					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// Not implemented
					}
				} };

			try {
				SSLContext sc = SSLContext.getInstance("TLS");

				sc.init(null, trustAllCerts, new java.security.SecureRandom());

				trustAllSocketFactory = sc.getSocketFactory();
			} catch (KeyManagementException e) {
				//e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				//e.printStackTrace();
			}
		}

		return trustAllSocketFactory;
	}
	
    public DownloadItem setFileName(String fileName) {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }
        this.fileName = fileName;
        mimeType = guessMimeTypeFromFileName(fileName, mimeType);
        updateInfoFromFile();
        return this;
    }

    public DownloadItem setFileName() {
        return setFileName(new File(url.getPath()).getName());
    }

    @Override
    public String toString() {
        return getFileName();
    }

    void fetchMetaData() {
        if (status == Status.STARTED) {
            throw new IllegalStateException("Downloading is in progress");
        }

        HttpURLConnection conn = null;

        try {
            conn = connect(openConnection("HEAD"));

            totalSize = getContentLengthFromConnection(conn);
            fileName = guessFileNameFromConnection(conn, fileName);
            mimeType = guessMimeTypeFromConnectionAndFileName(conn, fileName, mimeType, false);
            fileFolder = guessFileFolderFromMimeType(mimeType, fileFolder);
            updateInfoFromFile();

        } catch (IOException e) {
            android.util.Log.w("DownloadItem", "ERROR", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String guessMimeTypeFromFileName(String fileName, String def) {
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        return mimeType == null? def: mimeType;
    }

    private String guessMimeTypeFromConnectionAndFileName(URLConnection conn, String fileName, String def, boolean useContent) {
        android.util.Log.d("aGetGuessWork", "Default MIME: " + (def == null? "None": def));
        String mimeType = null;

        if (fileName != null) {
            mimeType = URLConnection.guessContentTypeFromName(fileName);
            android.util.Log.d("aGetGuessWork", "MIME from filename " + fileName + ": " + (mimeType == null? "None": mimeType));
        }

        if (mimeType == null) {
            mimeType = conn.getContentType();
            android.util.Log.d("aGetGuessWork", "MIME from header: " + (mimeType == null? "None": mimeType));
        }

        if (mimeType == null && useContent) {
            try {
                mimeType = URLConnection.guessContentTypeFromStream(conn.getInputStream());
            } catch (IOException e) {
                mimeType = null;
            }
        }

        android.util.Log.d("aGetGuessWork", "Resulting MIME: " + (mimeType == null? "default": mimeType));
        return mimeType == null? def: mimeType;
    }

    private long getContentLengthFromConnection(URLConnection conn) {
        String length = conn.getHeaderField("Content-Length");
        try {
            return length == null? -1: Long.parseLong(length, 10);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String guessFileNameFromConnection(URLConnection conn, String fileName) {
        String header = conn.getHeaderField("Content-Disposition");
        if (header == null) {
            header = conn.getHeaderField("Content-Type");
        }

        if (header != null) {
            fileName = getFileNameFromHeader(header, fileName);
        }

        return fileName;
    }

    private String guessFileFolderFromMimeType(String mimeType, String def) {
        return mimeType.startsWith("video/")? Environment.DIRECTORY_MOVIES:
               mimeType.startsWith("audio/")? Environment.DIRECTORY_MUSIC:
               mimeType.startsWith("image/")? Environment.DIRECTORY_PICTURES:
               def;
    }

    private String getFileNameFromHeader(String header, String def) {
        int p = header.indexOf("name=");
        if (p < 0) return def;

        header = header.substring(p + 5);
        if (header.charAt(0) == '"') {
            header = header.substring(1, header.length() - 1);
        }

        return header;
    }

    public DownloadItem cancelDownload(boolean deleteLocalFile) {
        status = Status.CANCELED;
        stopDownload();

        if (deleteLocalFile) {
            getFile().delete();
        }
        return this;
    }

    public DownloadItem cancelDownload() {
        return cancelDownload(false);
    }

    public DownloadItem pauseDownload() {
        status = Status.PAUSED;
        stopDownload();
        return this;
    }

    public DownloadItem stopDownload() {
        if (downloadThread != null && !downloadThread.isInterrupted()) {
            downloadThread.interrupt();
            downloadThread = null;
            System.gc();
        }
        return this;
    }

    public DownloadItem startDownload(final Listener listener) {
        downloadThread = new Thread(new Runnable () {
            @Override
            public void run() {
                try {
                    download(listener);
                } catch (RuntimeException e) {
                    android.util.Log.e("DownloadItem", "Error while downloading", e);
                }
            }
        });
        
        downloadThread.start();
        return this;
    }

    public String getFileFolder() {
        return fileFolder;
    }

    public DownloadItem setFileFolder(String folder) {
        this.fileFolder = folder;
        updateInfoFromFile();
        return this;
    }

    public DownloadItem setFileFolder() {
        fileFolder = guessFileFolderFromMimeType(mimeType, fileFolder);
        return this;
    }

    public DownloadItem setIgnoreCertificate(boolean ignoreCertificate) {
        this.ignoreCertificate = ignoreCertificate;
        return this;
    }

    public DownloadItem setIgnoreCertificate() {
        return setIgnoreCertificate(true);
    }

    public boolean isIgnoreCertificate() {
        return ignoreCertificate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getProgressInt() {
        return totalSize > 0? (int) (downloadedSize * 100 / totalSize): -1;
    }

    public float getProgress() {
        return totalSize > 0? (float) (downloadedSize * 100.0 / totalSize): -1;
    }

    public long getLeftSize() {
        return totalSize - downloadedSize;
    }

    public long getTimeLeft() {
        return lastSpeed > 0? (totalSize - downloadedSize) / lastSpeed: -1;
    }

    public boolean isUnkownSize() {
        return totalSize < 0;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof DownloadItem) && url.equals(((DownloadItem) other).getUrl());
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    private void updateInfoFromFile() {
        File file = getFile();
        downloadedSize = continueDownload && file.exists()? file.length(): 0;
        status = totalSize > 0 && downloadedSize >= totalSize? Status.FINISHED: Status.INITIAL;
    }
}
