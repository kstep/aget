package me.kstep.aget;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.googlecode.androidannotations.annotations.*;
import me.kstep.downloader.Downloader;
import me.kstep.downloader.Download;

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
    Button downloadStart;

    @ViewById
    Button downloadReload;

    @ViewById
    Button downloadPause;

    @ViewById
    Button downloadCancel;

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
            .setTitle("Remove download item?")
            .setMessage("You can re-add the same URL again with “Continue download” option on to resume downloading.")
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            })
            .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    try { item.stop(); } catch (IllegalStateException e) {}
                    adapter.removeItem(item);
                    requestFocusFromTouch();
                }
            })
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    try { item.cancel(); } catch (IllegalStateException e) {}
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
