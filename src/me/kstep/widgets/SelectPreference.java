package me.kstep.widgets;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SelectPreference extends ListPreference {
    public SelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = super.getSummary();
        if (summary == null) return null;

        String pattern = summary.toString();
        return pattern.contains("%")? String.format(pattern, getPersistedString("")): summary;
    }

    @Override
    protected void onDialogClosed(boolean isPositive) {
        super.onDialogClosed(isPositive);
        notifyChanged();
    }
}
