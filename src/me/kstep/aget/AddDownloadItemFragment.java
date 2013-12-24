package me.kstep.aget;

import android.app.DialogFragment;
import com.googlecode.androidannotations.annotations.*;
import android.widget.Spinner;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.CheckBox;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import java.net.MalformedURLException;
import android.os.Environment;

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
    void initBinding() {
        if (item == null) return;
        downloadName.setText(item.getFileName() == null? "": item.getFileName());
        downloadUrl.setText(item.getUrl() == null? "": item.getUrl().toString());
        downloadContinue.setChecked(item.getContinue());
        downloadFolder.setSelection(getFolderId(item.getFileFolder()));
    }

    @AfterViews
    void initButtons() {
        final MainActivity activity = (MainActivity) getActivity();

        getDialog().setTitle("Add new download");
        downloadCancelBtn.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
                dismiss();
            }
        });
        downloadEnqueueBtn.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
                try {
                    submit();
                    activity.downloadEnqueue(item);
                    dismiss();
                } catch (MalformedURLException e) {
                }
            }
        });
        downloadStartBtn.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
                try {
                    submit();
                    activity.downloadEnqueue(item);
                    activity.downloadStart(item);
                    dismiss();
                } catch (MalformedURLException e) {
                }
            }
        });
        fetchName.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
                fetchDownloadName();
            }
        });
    }

    @UiThread
    void setDownloadName(String name) {
        downloadName.setText(name);
    }

    @Background
    void fetchDownloadName() {
        setDownloadName(item.fetchFileName());
    }

    DownloadItem item = null;
    void bind(DownloadItem item) {
        this.item = item;
    }

    void submit(DownloadItem item) throws MalformedURLException {
        item.setUrl(downloadUrl.getText().toString());
        item.setFileName(downloadName.getText().toString());
        item.setContinue(downloadContinue.isChecked());
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
