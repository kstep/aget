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
        if (item == null) return;
        downloadName.setText(item.getFileName() == null? "": item.getFileName());
        downloadUrl.setText(item.getUrl() == null? "": item.getUrl().toString());
        downloadContinue.setChecked(item.getContinue());
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
				submit();
                activity.downloadEnqueue(item);
                dismiss();
            }
        });
        downloadStartBtn.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
				submit();
                activity.downloadEnqueue(item);
                activity.downloadStart(item);
                dismiss();
            }
        });
        fetchName.setOnClickListener(new View.OnClickListener () {
            public void onClick(View view) {
                fetchMetaData();
            }
        });
    }

    @Background
    void fetchMetaData() {
        item.fetchMetaData();
		initBinding();
    }

    DownloadItem item = null;
    void bind(DownloadItem item) {
        this.item = item;
    }
	
	void submit(DownloadItem item) {
		try {
			item.setUrl(downloadUrl.getText().toString());
		    item.setFileName(downloadName.getText().toString());
			item.setContinue(downloadContinue.isChecked());
			//item.setFileFolder();
		} catch (MalformedURLException e) {}
	}
	
	void submit() {
		submit(item);
	}
}
