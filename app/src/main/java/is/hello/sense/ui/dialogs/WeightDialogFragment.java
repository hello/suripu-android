package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.NumberPicker;

import is.hello.sense.R;

public class WeightDialogFragment extends DialogFragment {
    public static final String TAG = WeightDialogFragment.class.getSimpleName();

    public static final int DEFAULT_WEIGHT = 120;
    private static final String ARG_WEIGHT = WeightDialogFragment.class.getName() + ".ARG_WEIGHT";

    public static final String RESULT_WEIGHT = WeightDialogFragment.class.getName() + ".RESULT_WEIGHT";
    private NumberPicker numberPicker;

    public static WeightDialogFragment newInstance(long weight) {
        WeightDialogFragment dialogFragment = new WeightDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putLong(ARG_WEIGHT, weight);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.label_weight);
        this.numberPicker = new NumberPicker(getActivity());
        numberPicker.setMinValue(20);
        numberPicker.setMaxValue(500);
        numberPicker.setValue((int) getWeight());
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        builder.setView(numberPicker);

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, this::onPositiveButtonClicked);

        return builder.create();
    }


    public void onPositiveButtonClicked(@NonNull DialogInterface dialogInterface, int which) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(RESULT_WEIGHT, numberPicker.getValue());
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
    }

    private long getWeight() {
        return getArguments().getLong(ARG_WEIGHT, DEFAULT_WEIGHT);
    }
}
