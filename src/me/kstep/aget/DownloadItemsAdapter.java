package me.kstep.aget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

class DownloadItemsAdapter extends ArrayAdapter<DownloadItem> {
    private static LayoutInflater inflater;

    DownloadItemsAdapter(Context context, DownloadItem[] items) {
        super(context, R.layout.download_item, items);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        DownloadItem item = getItem(pos);

        if (view == null) {
            view = inflater.inflate(R.layout.download_item, null);
            view.setTag(item);
        }

        TextView tv = (TextView) view.findViewById(R.id.downloadFilename);
        tv.setText(item.getFileName());

        long totalSize = item.getTotalSize();
        long downloadedSize = item.getDownloadedSize();
        long lastSpeed = item.getLastSpeed();
        float percent = downloadedSize * 100 / totalSize;
        DownloadItem.Status status = item.getStatus();

        ProgressBar bar = (ProgressBar) view.findViewById(R.id.downloadProgress);
        bar.setIndeterminate(false);

        switch (status) {
            case FINISHED:
                bar.setProgress(100);
                break;

            case INITIAL:
                bar.setProgress(0);
                break;

            default:
                bar.setIndeterminate(totalSize < 0);
                bar.setProgress((int) percent);
        }

        TextView tvi = (TextView) view.findViewById(R.id.downloadInfo);
        tvi.setText(String.format("%s/%s — %s/s — %3.1f%%", humanize(downloadedSize), humanize(totalSize), humanize(lastSpeed), percent));

        Button pauseBtn = (Button) view.findViewById(R.id.downloadPause);
        Button startBtn = (Button) view.findViewById(R.id.downloadStart);
        Button reloadBtn = (Button) view.findViewById(R.id.downloadReload);

        switch (status) {
            case INITIAL:
            case PAUSED:
            case FAILED:
                pauseBtn.setVisibility(View.INVISIBLE);
                startBtn.setVisibility(View.VISIBLE);
                reloadBtn.setVisibility(View.INVISIBLE);
                break;

            case STARTED:
                pauseBtn.setVisibility(View.VISIBLE);
                startBtn.setVisibility(View.INVISIBLE);
                reloadBtn.setVisibility(View.INVISIBLE);
                break;

            case FINISHED:
                pauseBtn.setVisibility(View.INVISIBLE);
                startBtn.setVisibility(View.INVISIBLE);
                reloadBtn.setVisibility(View.VISIBLE);
                break;
        }

        return view;
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
