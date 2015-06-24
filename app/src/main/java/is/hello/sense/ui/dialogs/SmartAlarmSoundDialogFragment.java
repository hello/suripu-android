package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.adapter.SmartAlarmSoundAdapter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseListDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SoundPlayer;

public class SmartAlarmSoundDialogFragment extends InjectionDialogFragment implements SenseListDialog.Listener<Alarm.Sound>, SoundPlayer.OnEventListener {
    public static final String ARG_SELECTED_SOUND = SmartAlarmSoundDialogFragment.class.getName() + ".ARG_SELECTED_SOUND";

    public static final String TAG = SmartAlarmSoundDialogFragment.class.getSimpleName();

    @Inject SmartAlarmPresenter presenter;

    private Alarm.Sound selectedSound;
    private SmartAlarmSoundAdapter adapter;
    private SenseListDialog<Alarm.Sound> dialog;

    private SoundPlayer soundPlayer;

    public static SmartAlarmSoundDialogFragment newInstance(@Nullable Alarm.Sound sound) {
        SmartAlarmSoundDialogFragment dialogFragment = new SmartAlarmSoundDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SELECTED_SOUND, sound);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.soundPlayer = new SoundPlayer(getActivity(), this, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.dialog = new SenseListDialog<>(getActivity());

        this.selectedSound = (Alarm.Sound) getArguments().getSerializable(ARG_SELECTED_SOUND);

        this.adapter = new SmartAlarmSoundAdapter(getActivity());
        if (selectedSound != null) {
            adapter.setSelectedSoundId(selectedSound.id);
        }

        dialog.setMessage(R.string.title_sound);
        dialog.setListener(this);
        dialog.setAdapter(adapter);
        dialog.setDoneButtonEnabled(selectedSound != null);
        dialog.setActivityIndicatorVisible(true);

        bindAndSubscribe(presenter.availableAlarmSounds(), this::bindSounds, this::presentError);

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        soundPlayer.recycle();
    }

    public void bindSounds(@NonNull ArrayList<Alarm.Sound> sounds) {
        dialog.setActivityIndicatorVisible(false);
        adapter.addAll(sounds);
    }

    public void presentError(Throwable e) {
        dialog.setActivityIndicatorVisible(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
        dismiss();
    }


    private void promptToIncreaseVolume(@NonNull Alarm.Sound sound) {
        SenseAlertDialog prompt = new SenseAlertDialog(getActivity());
        prompt.setTitle(R.string.dialog_title_alarm_sound_volume_low);
        prompt.setMessage(R.string.dialog_message_alarm_sound_volume_low);
        prompt.setNegativeButton(R.string.dialog_negative_alarm_sound_volume_low, (dialog, which) -> {
            playSound(sound);
        });
        prompt.setPositiveButton(R.string.dialog_positive_alarm_sound_volume_low, (dialog, which) -> {
            int targetVolume = soundPlayer.getRecommendedStreamVolume();
            soundPlayer.setStreamVolume(targetVolume, AudioManager.FLAG_SHOW_UI);
            playSound(sound);
        });
        prompt.show();
    }

    @Override
    public void onItemClicked(@NonNull SenseListDialog<Alarm.Sound> dialog, int position, @NonNull Alarm.Sound sound) {
        this.selectedSound = sound;

        adapter.setSelectedSoundId(sound.id);
        getArguments().putSerializable(ARG_SELECTED_SOUND, selectedSound);
        dialog.setDoneButtonEnabled(true);

        if (soundPlayer.isStreamVolumeAdjustable() && soundPlayer.getStreamVolume() < soundPlayer.getRecommendedStreamVolume()) {
            promptToIncreaseVolume(sound);
        } else {
            playSound(sound);
        }
    }

    @Override
    public void onDoneClicked(@NonNull SenseListDialog<Alarm.Sound> dialog) {
        Intent response = new Intent();
        response.putExtra(ARG_SELECTED_SOUND, selectedSound);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
    }


    //region Playback

    public void playSound(@NonNull Alarm.Sound sound) {
        soundPlayer.play(Uri.parse(sound.url));
        adapter.setPlayingSoundId(sound.id, true);
    }


    @Override
    public void onPlaybackStarted(@NonNull SoundPlayer player) {
        adapter.setPlayingSoundId(adapter.getPlayingSoundId(), false);
    }

    @Override
    public void onPlaybackStopped(@NonNull SoundPlayer player, boolean finished) {
        adapter.setPlayingSoundId(SmartAlarmSoundAdapter.NONE, false);
    }

    @Override
    public void onPlaybackError(@NonNull SoundPlayer player, @NonNull Throwable error) {
        Analytics.trackError(error, "Alarm sound preview");

        Toast.makeText(getActivity().getApplicationContext(), R.string.error_failed_to_play_alarm_sound, Toast.LENGTH_SHORT).show();
        adapter.setPlayingSoundId(SmartAlarmSoundAdapter.NONE, false);
    }

    @Override
    public void onPlaybackPulse(@NonNull SoundPlayer player, int position) {
    }

    //endregion
}
