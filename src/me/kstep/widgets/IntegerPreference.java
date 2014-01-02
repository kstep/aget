package me.kstep.widgets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import me.kstep.aget.R;

public class IntegerPreference extends DialogPreference implements DialogInterface.OnClickListener, SeekBar.OnSeekBarChangeListener, TextView.OnEditorActionListener {

    public IntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context.obtainStyledAttributes(attrs, R.styleable.IntegerPreference, 0, 0));
    }

    public IntegerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context.obtainStyledAttributes(attrs, R.styleable.IntegerPreference, defStyle, 0));
    }


    private int minValue;
    private int maxValue;
    private int stepValue;
    private int defaultValue;
    private String unitName;
    private SeekBar seekbar;
    private EditText currentValueView;

    private void initialize(TypedArray attrs) {
        minValue = attrs.getInt(R.styleable.IntegerPreference_minValue, 0);
        maxValue = attrs.getInt(R.styleable.IntegerPreference_maxValue, 10);
        stepValue = attrs.getInt(R.styleable.IntegerPreference_stepValue, 1);
        unitName = attrs.getString(R.styleable.IntegerPreference_unitName);

        if (stepValue == 0) {
            stepValue = 1;
        }

        attrs.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.integer_preference_dialog, null);

        int currentValue = getPersistedInt(defaultValue);

        // Round to closest step
        currentValue = currentValue / stepValue * stepValue;

        seekbar = (SeekBar) view.findViewById(R.id.seek_bar);
        seekbar.setMax((maxValue - minValue) / stepValue);
        seekbar.setProgress((currentValue - minValue) / stepValue);
        seekbar.setOnSeekBarChangeListener(this);

        currentValueView = (EditText) view.findViewById(R.id.current_value);
        currentValueView.setText(String.valueOf(currentValue));
        currentValueView.setOnEditorActionListener(this);

        ((TextView) view.findViewById(R.id.min_value)).setText(String.valueOf(minValue));
        ((TextView) view.findViewById(R.id.max_value)).setText(String.valueOf(maxValue));

        if (unitName != null) {
            ((TextView) view.findViewById(R.id.unit_name)).setText(unitName);
        }

        return view;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray arr, int index) {
        defaultValue = arr.getInt(index, 0);
        return defaultValue;
    }

    @Override
    protected String getPersistedString(String defRetValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }

    @Override
    protected void onDialogClosed(boolean isPositive) {
        super.onDialogClosed(isPositive);

        if (isPositive && shouldPersist()) {
            int value = getValueFromInput();
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = super.getSummary();
        if (summary == null) return null;

        String pattern = summary.toString();
        return pattern.contains("%")? String.format(pattern, getPersistedInt(defaultValue)): summary;
    }

    @Override
    public void onStartTrackingTouch(SeekBar sb) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar sb) {
    }

    @Override
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        currentValueView.setText(String.valueOf(progress * stepValue + minValue));
    }

    private int getValueFromInput() {
        int value = 0;
        try {
            value = Integer.parseInt(currentValueView.getText().toString(), 10);
        } catch (NumberFormatException e) {
        }

        if (value < minValue) {
            value = minValue;
        } else if (value > maxValue) {
            value = maxValue;
        }

        // Round to closest step
        if (stepValue != 1) {
            value = value / stepValue * stepValue;
            currentValueView.setText(String.valueOf(value));
        }

        return value;
    }

    @Override
    public boolean onEditorAction(TextView tv, int actionId, KeyEvent ev) {
        int value = getValueFromInput();
        seekbar.setProgress((value - minValue) / stepValue);
        return true;
    }

}
