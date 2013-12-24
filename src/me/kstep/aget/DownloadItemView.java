package me.kstep.aget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
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

    @SuppressWarnings("fallthrough")
    public void bind(final DownloadItem item) {
        long totalSize = item.getTotalSize();
        long downloadedSize = item.getDownloadedSize();
        long lastSpeed = item.getLastSpeed();
        long percent = totalSize == 0? -1: downloadedSize * 100 / totalSize;
        DownloadItem.Status status = item.getStatus();

        downloadFilename.setText(item.getFileName());
        downloadInfo.setText(String.format("%s/%s — %s/s — %d%%", humanize(downloadedSize), humanize(totalSize), humanize(lastSpeed), percent));

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

    private String humanize(long value) {
        String[] suffixes = {"b", "K", "M", "G", "T"};
        String suffix;
        int index = 0;

        while (value > 1024 && index < suffixes.length) {
            value /= 1024;
            index++;
        }

        return value + suffixes[index];
    }
}
