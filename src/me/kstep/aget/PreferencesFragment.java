package me.kstep.aget;

import android.preference.PreferenceFragment;
import com.googlecode.androidannotations.annotations.*;
import android.os.Bundle;

@EFragment
class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.preferences);
    }
}
