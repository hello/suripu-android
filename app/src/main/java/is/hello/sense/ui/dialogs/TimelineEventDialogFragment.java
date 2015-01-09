package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;

public final class TimelineEventDialogFragment extends InjectionDialogFragment implements MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener {
    public static final String TAG = TimelineEventDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineEventDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject Markdown markdown;
    private TimelineSegment timelineSegment;

    private MediaPlayer soundPlayer;
    private ImageView soundPlayButton;
    private SeekBar soundSeekBar;


    public static TimelineEventDialogFragment newInstance(@NonNull TimelineSegment segment) {
        TimelineEventDialogFragment dialogFragment = new TimelineEventDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SEGMENT, segment);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.timelineSegment = (TimelineSegment) getArguments().getSerializable(ARG_SEGMENT);

        setCancelable(true);

        setRetainInstance(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Simple);

        dialog.setContentView(R.layout.dialog_fragment_timeline_event);

        ViewGroup container = (ViewGroup) dialog.findViewById(R.id.dialog_fragment_timeline_event_container);

        ImageButton closeButton = (ImageButton) dialog.findViewById(R.id.dialog_fragment_timeline_event_close);
        Views.setSafeOnClickListener(closeButton, ignored -> dismiss());

        TextView title = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_title);
        String eventName = getString(timelineSegment.getEventType().nameString);
        boolean use24Time = preferences.getBoolean(PreferencesPresenter.USE_24_TIME, false);
        String formattedTime = dateFormatter.formatAsTime(timelineSegment.getTimestamp(), use24Time);
        title.setText(getString(R.string.title_timeline_event_fmt, eventName, formattedTime));

        TextView message = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_message);
        markdown.render(timelineSegment.getMessage())
                .subscribe(message::setText, e -> message.setText(R.string.missing_data_placeholder));

        if (timelineSegment.getSound() == null) {
            View soundControls = LayoutInflater.from(getActivity()).inflate(R.layout.sub_fragment_timeline_event_sound, container, false);

            this.soundPlayButton = (ImageView) soundControls.findViewById(R.id.sub_fragment_timeline_event_sound_play);
            soundPlayButton.setOnClickListener(this::playSound);

            this.soundSeekBar = (SeekBar) soundControls.findViewById(R.id.sub_fragment_timeline_event_sound_seek_bar);
            soundSeekBar.setEnabled(false);
            soundSeekBar.setOnSeekBarChangeListener(this);

            container.addView(soundControls);
        }

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (soundPlayer != null) {
            soundPlayer.release();
        }
    }


    //region Sound Playback

    public void playSound(@NonNull View sender) {
        if (soundPlayer == null) {
            this.soundPlayer = new MediaPlayer();
            soundPlayer.setOnSeekCompleteListener(this);
            soundPlayer.setOnErrorListener(this);
            soundPlayer.setOnPreparedListener(this);
            soundPlayer.setOnCompletionListener(this);
            soundPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } else {
            soundPlayer.reset();
        }

        try {
            Uri soundUri = Uri.parse(timelineSegment.getSound().getUrl());
            soundPlayer.setDataSource(getActivity(), soundUri);
            soundPlayer.prepareAsync();

            soundPlayButton.setEnabled(false);
        } catch (IOException e) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }


    public void updatePlayButton() {
        if (soundPlayer.isPlaying()) {
            soundPlayButton.setImageResource(R.drawable.timeline_pause);
        } else {
            soundPlayButton.setImageResource(R.drawable.timeline_play);
        }
    }

    public void stopPlayback() {
        soundSeekBar.setEnabled(false);
        soundSeekBar.setProgress(0);
        soundPlayer.reset();
        updatePlayButton();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopPlayback();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        soundSeekBar.setProgress(mp.getCurrentPosition());
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        soundPlayer.start();

        soundSeekBar.setMax(soundPlayer.getDuration());
        soundSeekBar.setProgress(0);
        soundSeekBar.setEnabled(true);
        soundPlayButton.setEnabled(true);

        updatePlayButton();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        soundPlayer.seekTo(seekBar.getProgress());
    }

    //endregion
}
