package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.ui.adapter.SmartAlarmSoundAdapter;
import is.hello.sense.ui.common.InjectionDialogFragment;

public class SmartAlarmSoundDialogFragment extends InjectionDialogFragment {
    public static final String ARG_SELECTED_SOUND = SmartAlarmSoundDialogFragment.class.getName() + ".ARG_SELECTED_SOUND";

    public static final String TAG = SmartAlarmSoundDialogFragment.class.getSimpleName();

    @Inject ApiService apiService;

    public static SmartAlarmSoundDialogFragment newInstance(@Nullable SmartAlarm.Sound sound) {
        SmartAlarmSoundDialogFragment dialogFragment = new SmartAlarmSoundDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SELECTED_SOUND, sound);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        SmartAlarmSoundAdapter adapter = new SmartAlarmSoundAdapter(getActivity());

        SmartAlarm.Sound selectedSound = (SmartAlarm.Sound) getArguments().getSerializable(ARG_SELECTED_SOUND);
        if (selectedSound != null) {
            adapter.setSelectedSoundId(selectedSound.id);
        }

        builder.setAdapter(adapter, (dialog, which) -> {
            if (getTargetFragment() != null) {
                Intent response = new Intent();
                response.putExtra(ARG_SELECTED_SOUND, adapter.getItem(which));
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        bindAndSubscribe(apiService.availableSmartAlarmSounds(),
                         adapter::addAll,
                         e -> {});

        return builder.create();
    }
}
