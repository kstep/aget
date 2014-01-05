package me.kstep.aget;

import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.googlecode.androidannotations.annotations.*;
import com.googlecode.androidannotations.annotations.sharedpreferences.Pref;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import me.kstep.downloader.Download;
import me.kstep.downloader.Downloader;

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main_activity_actions)
public class DownloadManagerActivity extends ListActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Bean
    DownloadsAdapter adapter;

    @SystemService
    ConnectivityManager connectivity;

    ClipboardManager clipboard;

    @Pref
    Preferences_ prefs;

    @Extra(Intent.EXTRA_TEXT)
    String sharedText;

    @AfterInject
    void injectClipboardManager() {
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    @AfterViews
    void bindAdapter() {
        setListAdapter(adapter);
    }

    @AfterViews
    void processIntentUri() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            if (sharedText != null && !"".equals(sharedText)) {
                downloadAdd(sharedText);
            }

        } else if (Intent.ACTION_VIEW.equals(action)) {
            String uri = intent.getDataString();
            if (uri != null && !"".equals(uri)) {
                downloadAdd(uri);
            }
        }
    }

    @AfterViews
    void loadDownloadsList() {
        try {
            ObjectInputStream io = new ObjectInputStream(openFileInput("downloads.bin"));
            adapter.loadFromStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    @AfterInject
    void setupDownloadPrefs() {
        Downloader.setConnectTimeout(prefs.connectTimeout().get());
        Downloader.setReadTimeout(prefs.readTimeout().get());
        Downloader.setBufferSize(prefs.bufferSize().get());
        Downloader.setDefaultResume(prefs.continueDownload().get());
        Downloader.setDefaultInsecure(prefs.ignoreCertificates().get());
        Downloader.setUseWiFiOnly(prefs.useWiFiOnly().get());
    }

    @AfterInject
    void injectConnectivityManagerToDownloader() {
        Downloader.setConnectivity(connectivity);
    }

    private DownloadManagerService downloadService;
    private boolean downloadBound = false;
    private ServiceConnection downloadConnection = new ServiceConnection () {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            downloadService = (DownloadManagerService) ((DownloadManagerService.LocalBinder) binder).getService();
            adapter.setDownloadList(downloadService.getDownloadList());
            downloadBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            downloadBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, DownloadManagerService.class);
        startService(intent);
        bindService(intent, downloadConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadBound) {
            unbindService(downloadConnection);
            downloadBound = false;
        }
    }

    @AfterInject
    void registerPrefsListener() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        setupDownloadPrefs();
    }

    @Override
    protected void onPause() {
        try {
            ObjectOutputStream io = new ObjectOutputStream(openFileOutput("downloads.bin", MODE_PRIVATE));
            adapter.saveToStream(io);

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        super.onPause();
    }

    @OptionsItem
    void downloadAdd() {
        ClipData clip = clipboard.getPrimaryClip();
        String uri = clip == null? null: clip.getItemAt(0).coerceToText(this).toString();
        downloadAdd(uri);
    }

    private boolean preferencesVisible = false;
    @OptionsItem
    void downloadPrefs() {
        if (!preferencesVisible) {
            getFragmentManager().beginTransaction()
                .add(android.R.id.content, new PreferencesFragment_())
                .addToBackStack(null)
                .commit();
            preferencesVisible = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().popBackStackImmediate()) {
            preferencesVisible = false;
        } else {
            finish();
        }
    }

    @OptionsItem
    void home() {
        onBackPressed();
    }

    void downloadAdd(String uri) {
        DownloadItem downloadItem = new DownloadItem();
        if (uri != null) {
            downloadItem.setUri(uri);
            downloadItem.setFileNameFromUri();
            downloadItem.setFileFolderByExtension();
        }

        AddDownloadFragment addDownloadDialog = new AddDownloadFragment_();
        addDownloadDialog.bind(downloadItem);
        addDownloadDialog.show(getFragmentManager(), "addDownload");
    }

    @ItemClick
    public void listItemClicked(Download proxy) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(proxy.getItem().getFile()));
        startActivity(intent);
    }
}
