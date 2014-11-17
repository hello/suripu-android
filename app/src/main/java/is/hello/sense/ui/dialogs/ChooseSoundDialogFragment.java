package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

import is.hello.sense.ui.adapter.AlarmSoundAdapter;

public class ChooseSoundDialogFragment extends DialogFragment {
    public static final String ARG_SELECTED_ID = ChooseSoundDialogFragment.class.getName() + ".ARG_SELECTED_ID";

    public static final String TAG = ChooseSoundDialogFragment.class.getSimpleName();

    public static ChooseSoundDialogFragment newInstance(long selectedId) {
        ChooseSoundDialogFragment dialogFragment = new ChooseSoundDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putLong(ARG_SELECTED_ID, selectedId);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        AlarmSoundAdapter adapter = new AlarmSoundAdapter(getActivity());
        adapter.setSelectedSoundId(getArguments().getLong(ARG_SELECTED_ID, 0));
        builder.setAdapter(adapter, (dialog, which) -> {
            if (getTargetFragment() != null) {
                Intent response = new Intent();
                response.putExtra(ARG_SELECTED_ID, adapter.getItem(which).id);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
