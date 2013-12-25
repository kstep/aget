package me.kstep.aget;

import android.app.ListActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.googlecode.androidannotations.annotations.*;

@EViewGroup(R.layout.download_item)
public class DownloadItemView extends RelativeLayout {
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

    public DownloadItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DownloadItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DownloadItemView(Context context) {
        super(context);
    }

    ListView getListView() {
        return (ListView) getParent();
    }

    DownloadItem getListItem() {
        return (DownloadItem) ((ListActivity) getContext()).getListAdapter().getItem(getListView().getPositionForView(this));
    }

    DownloadItemsAdapter getListAdapter() {
        return (DownloadItemsAdapter) ((ListActivity) getContext()).getListAdapter();
    }

    @Click
    void downloadStart(View view) {
        getListItem().startDownload((DownloadItem.Listener) getContext());
    }

    @Click
    void downloadCancel(View view) {
        DownloadItem item = getListItem();
        item.cancelDownload();
        getListAdapter().removeItem(item);
        requestFocusFromTouch();
    }

    @Click
    void downloadPause(View view) {
        DownloadItem item = getListItem();
        item.pauseDownload();
        bind(item);
        requestFocusFromTouch();
    }

    @SuppressWarnings("fallthrough")
    public void bind(final DownloadItem item) {
        long totalSize = item.getTotalSize();
        long downloadedSize = item.getDownloadedSize();
        long lastSpeed = item.getLastSpeed();
        long percent = totalSize <= 0? -1: downloadedSize * 100 / totalSize;
        long timeLeft = lastSpeed > 0? (totalSize - downloadedSize) / lastSpeed: -1;
        DownloadItem.Status status = item.getStatus();

        downloadFilename.setText(item.getFileName());
        downloadSize.setText(String.format("%s/%s @ %s/s", humanize(downloadedSize), humanize(totalSize), humanize(lastSpeed)));
        downloadInfo.setText(String.format("%s — %d%%", humanizeTime(timeLeft), percent));

        downloadProgress.setIndeterminate(false);
        switch (status) {
            case FINISHED:
                downloadProgress.setProgress(100);

                downloadPause.setVisibility(View.INVISIBLE);
                downloadStart.setVisibility(View.INVISIBLE);
                downloadReload.setVisibility(View.VISIBLE);
                break;

            case INITIAL:
                downloadProgress.setProgress(0);

            case PAUSED:
            case FAILED:
                downloadPause.setVisibility(View.INVISIBLE);
                downloadStart.setVisibility(View.VISIBLE);
                downloadReload.setVisibility(View.INVISIBLE);
                break;

            case STARTED:
                downloadPause.setVisibility(View.VISIBLE);
                downloadStart.setVisibility(View.INVISIBLE);
                downloadReload.setVisibility(View.INVISIBLE);

                downloadProgress.setIndeterminate(totalSize < 0);
                downloadProgress.setProgress((int) percent);
                break;
        }
    }

    private String humanize(String format, long value) {
        String[] suffixes = {"b", "K", "M", "G", "T"};
        String suffix;
        int index = 0;
        float fvalue = (float) value;

        while (fvalue > 1024 && index < suffixes.length) {
            fvalue /= 1024;
            index++;
        }

        return String.format(format, fvalue, suffixes[index]);
    }

    private String humanize(long value) {
        return humanize("%3.2f%s", value);
    }

    private String humanizeTime(long time) {
        if (time <= 0) {
            return "";
        }

        final long[] coeffs = {
            86400L*7, // weeks
            86400L, // days
            3600L, // hours
            60L, // minutes
            1L, // seconds
        };
        final String[] names = {
            "w", "d", "h", "m", "s",
        };

        int items = 0;
        long value = 0;
        String result = "";

        for (int i = 0; i < coeffs.length; i++) {
            value = time / coeffs[i];
            time %= coeffs[i];

            if (value != 0) {
                result += value + names[i];
                if (++items > 1) {
                    break;
                }
            }
        }

        return result;
    }
}
