package me.kstep.downloader;

import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

public class ApacheHttpDownloader extends Downloader {

    @Override
    protected InputStream openConnection(Uri uri, File file) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uri.toString());
        int expected_code = 200;
        if (resume && file.exists()) {
            downloadedSize = file.length();
            request.addHeader("Range", "bytes=" + downloadedSize + "-");
            expected_code = 206;
        }

        HttpResponse response = client.execute(request);
        int code = response.getStatusLine().getStatusCode();

        if (code != expected_code) {
            throw new IOException(String.format("Invalid return code %d, expected %d", code, expected_code));
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new IOException("No result");
        }

        totalSize = entity.getContentLength();
        if (totalSize >= 0) {
            totalSize += downloadedSize;
        }

        return response.getContent();
    }

    @Override
    protected void closeConnection(InputStream is) {
    }

    @Override
    public FileMetaInfo getMetaInfo(Uri uri, File file) {
        FileMetaInfo meta = new FileMetaInfo();
        HttpClient client = new DefaultHttpClient();
        HttpHead request = new HttpHead(uri.toString());
        HttpResponse response = client.execute(request);

        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            throw new IOException(String.format("Invalid return code %d, expected 200", code));
        }

        try {
            meta.mimeType = response.getFirstHeader("Content-Type").getElements()[0].getValue();
        } catch (NullPointerException e) {
        }

        try {
            meta.totalSize = Long.parseLong(response.getFirstHeader("Content-Length").getValue(), 10);
        } catch (NullPointerException e) {
        }

        try {
            meta.fileName = response.getFirstHeader("Content-Disposition").getElements()[0].getParameterByName("filename").getValue();
        } catch (NullPointerException e) {
            try {
                meta.fileName = response.getFirstHeader("Content-Type").getElements()[0].getParameterByName("name").getValue();
            } catch (NullPointerException e) {
            }
        }

        return meta;
    }
}
