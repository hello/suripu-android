package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import is.hello.sense.R;

public class HeightDialogFragment extends DialogFragment {
    public static final String TAG = HeightDialogFragment.class.getSimpleName();

    private static final String ARG_HEIGHT = HeightDialogFragment.class.getName() + ".ARG_HEIGHT";

    public static final String RESULT_HEIGHT = HeightDialogFragment.class.getName() + ".RESULT_HEIGHT";

    private NumberPicker feetPicker;
    private NumberPicker inchesPicker;

    public static HeightDialogFragment newInstance(long heightInInches) {
        HeightDialogFragment heightDialogFragment = new HeightDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putLong(ARG_HEIGHT, heightInInches);
        heightDialogFragment.setArguments(arguments);

        return heightDialogFragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.label_height);

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1f;

        long heightInInches = getArguments().getLong(ARG_HEIGHT, 70);
        long feet = heightInInches / 12;
        long inches = heightInInches % 12;

        this.feetPicker = new NumberPicker(getActivity());
        feetPicker.setFormatter(value -> value + "'");
        feetPicker.setMinValue(1);
        feetPicker.setMaxValue(12);
        feetPicker.setValue((int) feet);
        feetPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        layout.addView(feetPicker, layoutParams);

        this.inchesPicker = new NumberPicker(getActivity());
        inchesPicker.setWrapSelectorWheel(false);
        inchesPicker.setFormatter(value -> value + "''");
        inchesPicker.setMinValue(0);
        inchesPicker.setMaxValue(12);
        inchesPicker.setValue((int) inches);
        inchesPicker.setOnScrollListener(this::onInchesPickerScrollStateChanged);
        inchesPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        layout.addView(inchesPicker, layoutParams);

        builder.setView(layout);

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, this::onPositiveButtonClicked);

        return builder.create();
    }


    public void onPositiveButtonClicked(DialogInterface dialogInterface, int which) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(RESULT_HEIGHT, calculateHeightInInches());
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
    }

    public void onInchesPickerScrollStateChanged(NumberPicker view, int scrollState) {
        if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
            if (view.getValue() == 12) {
                inchesPicker.setValue(0);
                feetPicker.setValue(feetPicker.getValue() + 1);
            }
        }
    }


    private int calculateHeightInInches() {
        return inchesPicker.getValue() + (feetPicker.getValue() * 12);
    }
}
