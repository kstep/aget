package me.kstep.aget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.googlecode.androidannotations.annotations.*;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloader;

@EFragment(R.layout.add_download_item)
class AddDownloadFragment extends DialogFragment {

    @ViewById
    Spinner downloadFolder;

    @ViewById
    EditText downloadName;

    @ViewById
    EditText downloadUrl;

    @ViewById
    CheckBox downloadContinue;

    @ViewById
    CheckBox downloadIgnoreCert;

    @ViewById
    Button downloadCancelBtn;

    @ViewById
    Button downloadEnqueueBtn;

    @ViewById
    Button downloadStartBtn;

    @ViewById
    Button fetchName;

    final private static String[] FOLDERS = {
        Environment.DIRECTORY_DOWNLOADS,
        Environment.DIRECTORY_MOVIES,
        Environment.DIRECTORY_MUSIC,
        Environment.DIRECTORY_PICTURES,
        Environment.DIRECTORY_PODCASTS,
    };

    @AfterViews
    void initSpinner() {
        final String[] folders = {
            "Downloads",
            "Movies",
            "Music",
            "Pictures",
            "Podcasts",
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, folders);
        downloadFolder.setAdapter(adapter);
    }

    @AfterViews
    @UiThread
    void initBinding() {
        getDialog().setTitle("Add new download");

        if (download == null) return;

        DownloadItem item = (DownloadItem) download.getItem();
        Downloader downloader = download.getDownloader();

        downloadUrl.setText(item.getUri() == null? "": item.getUri().toString());
        downloadName.setText(item.getFileName() == null? "": item.getFileName());
        downloadFolder.setSelection(getFolderId(item.getFileFolder()));

        downloadContinue.setChecked(downloader.isResume());
        downloadIgnoreCert.setChecked(downloader.isInsecure());
    }

    DownloadsAdapter getListAdapter() {
        return (DownloadsAdapter) ((ListActivity) getActivity()).getListAdapter();
    }

    @Click
    void downloadCancelBtn() {
        dismiss();
    }

    @Click
    void downloadEnqueueBtn() {
        submit();
        getListAdapter().addItem(download);
        dismiss();
    }

    @Click
    void downloadStartBtn() {
        submit();
        getListAdapter().addItem(download);
        download.start();
        dismiss();
    }

    @Click
    @Background
    void fetchName() {
        DownloadItem item = (DownloadItem) download.getItem();
        Downloader.FileMetaInfo meta = download.getDownloader().getMetaInfo(item.getUri(), item.getFile());
        item.setFileName(meta.fileName);
        item.setFileFolderByExtension();
        initBinding();
    }

    Download download = null;
    void bind(DownloadItem item) {
        download = new Download(item);
    }

    void submit(Download download) {
        download.setListener((Download.Listener) getActivity());

        DownloadItem item = (DownloadItem) download.getItem();
        item.setUri(downloadUrl.getText().toString());
        item.setFileName(downloadName.getText().toString());
        item.setFileFolder(getFolderHandle(downloadFolder.getSelectedItemPosition()));

        Downloader downloader = download.getDownloader();
        downloader.setInsecure(downloadIgnoreCert.isChecked());
        downloader.setResume(downloadContinue.isChecked());
    }

    void submit() {
        submit(download);
    }

    String getFolderHandle(int id) {
        try {
            return FOLDERS[id];
        } catch (ArrayIndexOutOfBoundsException e) {
            return Environment.DIRECTORY_DOWNLOADS;
        }
    }

    int getFolderId(String folder) {
        for (int index = 0; index < FOLDERS.length; index++) {
            if (FOLDERS[index] == folder) {
                return index;
            }
        }
        return 0;
    }
}
