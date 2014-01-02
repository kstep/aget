package me.kstep.aget;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.googlecode.androidannotations.annotations.*;

@EFragment
class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        View view = super.onCreateView(inflater, parent, state);
        if (view != null) {
            view.setBackgroundColor(0xffffffff);
            view.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                        ));
        }
        return view;
    }
}
