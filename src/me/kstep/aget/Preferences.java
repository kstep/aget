package me.kstep.aget;

import com.googlecode.androidannotations.annotations.sharedpreferences.*;

@SharedPref(value=SharedPref.Scope.APPLICATION_DEFAULT)
public interface Preferences {
    @DefaultInt(10240)
    int bufferSize();

    @DefaultInt(2000)
    int connectTimeout();

    @DefaultInt(5000)
    int readTimeout();

    @DefaultBoolean(true)
    boolean continueDownload();

    @DefaultBoolean(true)
    boolean ignoreCertificates();

    @DefaultBoolean(true)
    boolean useWiFiOnly();
}

