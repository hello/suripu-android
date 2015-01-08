package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.ui.adapter.SmartAlarmSoundAdapter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.SenseSelectorDialog;

public class SmartAlarmSoundDialogFragment extends InjectionDialogFragment implements SenseSelectorDialog.OnSelectionListener<SmartAlarm.Sound> {
    public static final String ARG_SELECTED_SOUND = SmartAlarmSoundDialogFragment.class.getName() + ".ARG_SELECTED_SOUND";

    public static final String TAG = SmartAlarmSoundDialogFragment.class.getSimpleName();

    @Inject ApiService apiService;

    private SmartAlarm.Sound selectedSound;
    private SmartAlarmSoundAdapter adapter;
    private SenseSelectorDialog<SmartAlarm.Sound> dialog;

    public static SmartAlarmSoundDialogFragment newInstance(@Nullable SmartAlarm.Sound sound) {
        SmartAlarmSoundDialogFragment dialogFragment = new SmartAlarmSoundDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SELECTED_SOUND, sound);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.dialog = new SenseSelectorDialog<>(getActivity());

        this.selectedSound = (SmartAlarm.Sound) getArguments().getSerializable(ARG_SELECTED_SOUND);

        this.adapter = new SmartAlarmSoundAdapter(getActivity());
        if (selectedSound != null) {
            adapter.setSelectedSoundId(selectedSound.id);
        }

        dialog.setOnSelectionListener(this);
        dialog.setAdapter(adapter);
        dialog.setDoneButtonEnabled(false);
        dialog.setActivityIndicatorVisible(true);

        bindAndSubscribe(apiService.availableSmartAlarmSounds(), this::bindSounds, this::presentError);

        return dialog;
    }


    public void bindSounds(@NonNull ArrayList<SmartAlarm.Sound> sounds) {
        dialog.setActivityIndicatorVisible(false);
        adapter.addAll(sounds);
    }

    public void presentError(Throwable e) {
        dialog.setActivityIndicatorVisible(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
        dismiss();
    }


    @Override
    public void onItemSelected(@NonNull SenseSelectorDialog<SmartAlarm.Sound> dialog, int position, @NonNull SmartAlarm.Sound item) {
        this.selectedSound = item;
        adapter.setSelectedSoundId(item.id);
        getArguments().putSerializable(ARG_SELECTED_SOUND, selectedSound);
        dialog.setDoneButtonEnabled(true);
    }

    @Override
    public void onSelectionCompleted(@NonNull SenseSelectorDialog<SmartAlarm.Sound> dialog) {
        Intent response = new Intent();
        response.putExtra(ARG_SELECTED_SOUND, selectedSound);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
    }
}
