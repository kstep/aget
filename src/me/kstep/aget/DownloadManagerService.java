package me.kstep.aget;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import com.googlecode.androidannotations.annotations.*;

@EService
public class DownloadManagerService extends Service {
    @SystemService
    NotificationManager notificationManager;

    public DownloadManagerService() {
        super(DownloadManagerService.class.getSimpleName());
    }
}
