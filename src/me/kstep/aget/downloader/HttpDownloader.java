package me.kstep.aget.downloader;

import android.net.Uri;
import android.util.Base64;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpDownloader extends Downloader {
    private boolean ignoreCertificate = true;

    private HttpURLConnection connection;

    @Override
    protected InputStream openConnection(Uri uri, File file) throws IOException {
        HttpURLConnection conn = openConnection(uri);
        int success_code = HttpURLConnection.HTTP_OK;
        if (resume && file.exists()) {
            success_code = HttpURLConnection.HTTP_PARTIAL;
            downloadedSize = file.length();
            conn.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
        }

        connect(conn, success_code);
        totalSize = parseContentLength(conn.getHeaderField("Content-Length"));
        if (totalSize >= 0) {
            totalSize += downloadedSize;
        }

        connection = conn;
        return conn.getInputStream();
    }

    @Override
    protected void closeConnection() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    private HttpURLConnection connect(HttpURLConnection conn) throws IOException {
        return connect(conn, HttpURLConnection.HTTP_OK);
    }

    private HttpURLConnection connect(HttpURLConnection conn, int expectedCode) throws IOException {
        conn.connect();
        int code = conn.getResponseCode();
        if (code != expectedCode) {
            throw new IOException(String.format("Invalid return code %d, expected %d", code, expectedCode));
        }
        return conn;
    }

    private HttpURLConnection openConnection(Uri uri, String method) throws IOException {
        URL url = new URL(uri.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        if (conn instanceof HttpsURLConnection && ignoreCertificate) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(getTrustAllSocketFactory());
        }

        if (url.getUserInfo() != null) {
            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(url.getUserInfo().getBytes(), Base64.NO_WRAP));
        }

        return conn;
    }
    private HttpURLConnection openConnection(Uri uri) throws IOException {
        return openConnection(uri, "GET");
    }

    private long parseContentLength(String length) {
        try {
            return length == null? -1: Long.parseLong(length, 10);
        } catch (NumberFormatException e) {
            return -1;
        }
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

    @Override
    public FileMetaInfo getMetaInfo(Uri uri) {
        FileMetaInfo meta = new FileMetaInfo();

        try {
            HttpURLConnection conn = openConnection(uri, "HEAD");
            meta.mimeType = conn.getHeaderField("Content-Type");
            meta.totalSize = parseContentLength(conn.getHeaderField("Content-Length"));

            String header = conn.getHeaderField("Content-Disposition");
            if (header != null) {
                meta.fileName = getFileNameFromHeader(header);
            }

        } catch (IOException e) {
        }

        return meta;
    }

    private String getFileNameFromHeader(String header) {
        int p = header.indexOf("name=");
        if (p < 0) return null;

        header = header.substring(p + 5);
        if (header.charAt(0) == '"') {
            header = header.substring(1, header.length() - 1);
        }

        return header;
    }
}
