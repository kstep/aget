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
import java.net.MalformedURLException;

@EFragment(R.layout.add_download_item)
class AddDownloadItemFragment extends DialogFragment {

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

        if (item == null) return;
        downloadName.setText(item.getFileName() == null? "": item.getFileName());
        downloadUrl.setText(item.getUrl() == null? "": item.getUrl().toString());
        downloadContinue.setChecked(item.isContinue());
		downloadIgnoreCert.setChecked(item.isIgnoreCertificate());
        downloadFolder.setSelection(getFolderId(item.getFileFolder()));
    }

    DownloadItemsAdapter getListAdapter() {
        return (DownloadItemsAdapter) ((ListActivity) getActivity()).getListAdapter();
    }

    @Click
    void downloadCancelBtn() {
        dismiss();
    }

    @Click
    void downloadEnqueueBtn() {
        try {
            submit();
            getListAdapter().addItem(item);
            dismiss();

        } catch (MalformedURLException e) {
            Toast.makeText(getActivity(), "Invalid URL", Toast.LENGTH_LONG).show();
        }
    }

    @Click
    void downloadStartBtn() {
        try {
            submit();
            getListAdapter().addItem(item);
            item.startDownload((DownloadItem.Listener) getActivity());
            dismiss();

        } catch (MalformedURLException e) {
            Toast.makeText(getActivity(), "Invalid URL", Toast.LENGTH_LONG).show();
        }
    }

    @Click
    @Background
    void fetchName() {
        item.fetchMetaData();
        initBinding();
    }

    DownloadItem item = null;
    void bind(DownloadItem item) {
        this.item = item;
    }

    void submit(DownloadItem item) throws MalformedURLException {
        item.setUrl(downloadUrl.getText().toString());
        item.setFileName(downloadName.getText().toString());
        item.setContinue(downloadContinue.isChecked());
		item.setIgnoreCertificate(item.isIgnoreCertificate());
        item.setFileFolder(getFolderHandle(downloadFolder.getSelectedItemPosition()));
    }

    void submit() throws MalformedURLException {
        submit(item);
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
