package me.kstep.aget;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.googlecode.androidannotations.annotations.*;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloader;

@EViewGroup(R.layout.download_item)
public class DownloadView extends RelativeLayout {
    @ViewById
    TextView downloadFilename;

    @ViewById
    TextView downloadInfo;

    @ViewById
    TextView downloadSize;

    @ViewById
    ProgressBar downloadProgress;

    @ViewById
    ImageButton downloadStart;

    @ViewById
    ImageButton downloadReload;

    @ViewById
    ImageButton downloadPause;

    @ViewById
    ImageButton downloadCancel;

    public DownloadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DownloadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DownloadView(Context context) {
        super(context);
    }

    ListView getListView() {
        return (ListView) getParent();
    }

    Download getListItem() {
        return (Download) ((ListActivity) getContext()).getListAdapter().getItem(getListView().getPositionForView(this));
    }

    DownloadsAdapter getListAdapter() {
        return (DownloadsAdapter) ((ListActivity) getContext()).getListAdapter();
    }

    @Click
    void downloadStart(View view) {
        getListItem().start();
    }

    @Click
    void downloadCancel(View view) {
        final Download item = getListItem();
        final DownloadsAdapter adapter = getListAdapter();

        new AlertDialog.Builder(getContext())
            .setTitle(R.string.remove_question)
            .setMessage(R.string.remove_message)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            })
            .setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    item.stop();
                    adapter.removeItem(item);
                    requestFocusFromTouch();
                }
            })
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    item.cancel();
                    adapter.removeItem(item);
                    requestFocusFromTouch();
                }
            })
            .show();
    }

    @Click
    void downloadPause(View view) {
        Download item = getListItem();
        item.pause();
        bind(item);
        requestFocusFromTouch();
    }

    @SuppressWarnings("fallthrough")
    public void bind(final Download download) {
        Downloader downloader = download.getDownloader();
        DownloadItem item = (DownloadItem) download.getItem();

        long totalSize = downloader.getTotalSize();
        long downloadedSize = downloader.getDownloadedSize();
        long lastSpeed = downloader.getLastSpeed();
        int percent = downloader.getProgressInt();
        long timeLeft = downloader.getTimeLeft();

        int status = download.getStatus();

        downloadFilename.setText(item.getFileName());
        downloadSize.setText(String.format("%s/%s @ %s/s", Util.humanizeSize(downloadedSize), Util.humanizeSize(totalSize), Util.humanizeSize(lastSpeed)));
        downloadInfo.setText(String.format("%s | %d%%", Util.humanizeTime(timeLeft), percent));

        downloadProgress.setIndeterminate(false);
        switch (status) {
            case Download.Status.FINISHED:
                downloadProgress.setProgress(100);

                downloadPause.setVisibility(View.INVISIBLE);
                downloadStart.setVisibility(View.INVISIBLE);
                downloadReload.setVisibility(View.VISIBLE);
                break;

            case Download.Status.INITIAL:
                downloadProgress.setProgress(0);

            case Download.Status.PAUSED:
            case Download.Status.FAILED:
                downloadPause.setVisibility(View.INVISIBLE);
                downloadStart.setVisibility(View.VISIBLE);
                downloadReload.setVisibility(View.INVISIBLE);
                break;

            case Download.Status.STARTED:
                downloadPause.setVisibility(View.VISIBLE);
                downloadStart.setVisibility(View.INVISIBLE);
                downloadReload.setVisibility(View.INVISIBLE);

                downloadProgress.setIndeterminate(downloader.isUnknownSize());
                downloadProgress.setProgress(percent);
                break;
        }
    }

}
