package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.ui.adapter.SmartAlarmSoundAdapter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.SenseSelectorDialog;

public class SmartAlarmSoundDialogFragment extends InjectionDialogFragment implements SenseSelectorDialog.OnSelectionListener<SmartAlarm.Sound>, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String ARG_SELECTED_SOUND = SmartAlarmSoundDialogFragment.class.getName() + ".ARG_SELECTED_SOUND";

    public static final String TAG = SmartAlarmSoundDialogFragment.class.getSimpleName();

    @Inject ApiService apiService;

    private SmartAlarm.Sound selectedSound;
    private SmartAlarmSoundAdapter adapter;
    private SenseSelectorDialog<SmartAlarm.Sound> dialog;

    private MediaPlayer soundPlayer;

    public static SmartAlarmSoundDialogFragment newInstance(@Nullable SmartAlarm.Sound sound) {
        SmartAlarmSoundDialogFragment dialogFragment = new SmartAlarmSoundDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SELECTED_SOUND, sound);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.soundPlayer = new MediaPlayer();

        soundPlayer.setOnPreparedListener(this);
        soundPlayer.setOnCompletionListener(this);
        soundPlayer.setOnErrorListener(this);
        soundPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopPlayback();
        soundPlayer.release();
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
    public void onItemSelected(@NonNull SenseSelectorDialog<SmartAlarm.Sound> dialog, int position, @NonNull SmartAlarm.Sound sound) {
        this.selectedSound = sound;
        adapter.setSelectedSoundId(sound.id);
        getArguments().putSerializable(ARG_SELECTED_SOUND, selectedSound);
        dialog.setDoneButtonEnabled(true);

        playSound(sound);
    }

    @Override
    public void onSelectionCompleted(@NonNull SenseSelectorDialog<SmartAlarm.Sound> dialog) {
        Intent response = new Intent();
        response.putExtra(ARG_SELECTED_SOUND, selectedSound);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
    }


    //region Playback

    public void playSound(@NonNull SmartAlarm.Sound sound) {
        stopPlayback();

        try {
            soundPlayer.setDataSource(getActivity(), Uri.parse(sound.url));
            soundPlayer.prepareAsync();
            adapter.setPlayingSoundId(sound.id, true);
        } catch (IOException e) {
            presentError(e);
        }
    }

    public void stopPlayback() {
        if (soundPlayer.isPlaying()) {
            soundPlayer.stop();
        }

        soundPlayer.reset();

        adapter.setPlayingSoundId(SmartAlarmSoundAdapter.NONE, false);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.i(getClass().getSimpleName(), "onPrepared");

        adapter.setPlayingSoundId(adapter.getPlayingSoundId(), false);
        soundPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.i(getClass().getSimpleName(), "onCompletion");

        stopPlayback();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        if (what != -38) {
            stopPlayback();

            Toast.makeText(getActivity().getApplicationContext(), R.string.error_failed_to_play_alarm_sound, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    //endregion
}
