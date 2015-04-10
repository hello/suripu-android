package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;
import is.hello.sense.util.SoundPlayer;

public final class TimelineEventDialogFragment extends InjectionDialogFragment implements SeekBar.OnSeekBarChangeListener, SoundPlayer.OnEventListener {
    public static final String TAG = TimelineEventDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineEventDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";


    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject Markdown markdown;
    private TimelineSegment timelineSegment;

    private SoundPlayer soundPlayer;
    private ImageView soundPlayButton;
    private SeekBar soundSeekBar;
    private ValueAnimator soundPlayPulseAnimator;
    private TextView title;
    private TextView message;


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

        this.title = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_title);
        this.message = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_message);

        if (timelineSegment.getSound() != null) {
            View soundControls = LayoutInflater.from(getActivity()).inflate(R.layout.sub_fragment_timeline_event_sound, container, false);

            this.soundPlayButton = (ImageView) soundControls.findViewById(R.id.sub_fragment_timeline_event_sound_play);
            soundPlayButton.setOnClickListener(this::playSound);

            this.soundSeekBar = (SeekBar) soundControls.findViewById(R.id.sub_fragment_timeline_event_sound_seek_bar);
            soundSeekBar.setEnabled(false);
            soundSeekBar.setOnSeekBarChangeListener(this);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                int tintColor = getResources().getColor(R.color.light_accent);
                soundSeekBar.getThumb().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
                soundSeekBar.getProgressDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }

            container.addView(Styles.createHorizontalDivider(getActivity(), ViewGroup.LayoutParams.MATCH_PARENT));
            container.addView(soundControls);
        }

        bindTimelineSegment();

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (soundPlayer != null) {
            stopPulsingPlayButton();
            soundPlayer.recycle();
        }
    }


    //region Binding

    private void bindTimelineSegment() {
        String eventName = getString(timelineSegment.getEventType().nameString);
        boolean use24Time = preferences.getUse24Time();
        String formattedTime = dateFormatter.formatAsTime(timelineSegment.getShiftedTimestamp(), use24Time);

        title.setText(getString(R.string.title_timeline_event_fmt, eventName, formattedTime));
        markdown.renderInto(message, timelineSegment.getMessage());
    }

    //endregion


    //region Sound Playback

    public void playSound(@NonNull View sender) {
        if (soundPlayer == null) {
            this.soundPlayer = new SoundPlayer(getActivity(), this);
        }

        if (soundPlayer.isPaused() || soundPlayer.isPlaying()) {
            soundPlayer.togglePaused();
        } else {
            Uri soundUri = Uri.parse(timelineSegment.getSound().getUrl());
            soundPlayer.play(soundUri);
            soundPlayButton.setEnabled(false);
            pulsePlayButton();
        }

        updatePlayButton();
    }


    public void pulsePlayButton() {
        this.soundPlayPulseAnimator = ValueAnimator.ofFloat(1f, 0.25f);
        soundPlayPulseAnimator.setDuration(500);
        soundPlayPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        soundPlayPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        soundPlayPulseAnimator.addUpdateListener(a -> {
            float alpha = (float) a.getAnimatedValue();
            soundPlayButton.setAlpha(alpha);
        });
        soundPlayPulseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                soundPlayButton.setAlpha(1f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                soundPlayButton.setAlpha(1f);
            }
        });
        soundPlayPulseAnimator.start();
    }

    public void stopPulsingPlayButton() {
        if (soundPlayPulseAnimator != null) {
            soundPlayPulseAnimator.cancel();
            this.soundPlayPulseAnimator = null;
        }
    }

    public void updatePlayButton() {
        if (soundPlayer.isPlaying() && !soundPlayer.isPaused()) {
            soundPlayButton.setImageResource(R.drawable.timeline_pause);
        } else {
            soundPlayButton.setImageResource(R.drawable.timeline_play);
        }
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


    @Override
    public void onPlaybackStarted(@NonNull SoundPlayer player) {
        soundSeekBar.setMax(soundPlayer.getDuration());
        soundSeekBar.setProgress(0);
        soundSeekBar.setEnabled(true);
        soundPlayButton.setEnabled(true);

        stopPulsingPlayButton();
        updatePlayButton();
    }

    @Override
    public void onPlaybackStopped(@NonNull SoundPlayer player, boolean finished) {
        soundSeekBar.setEnabled(false);
        soundSeekBar.setProgress(0);

        stopPulsingPlayButton();
        updatePlayButton();
    }

    @Override
    public void onPlaybackError(@NonNull SoundPlayer player, @NonNull Throwable error) {
        soundSeekBar.setEnabled(false);
        soundSeekBar.setProgress(0);

        stopPulsingPlayButton();
        updatePlayButton();

        ErrorDialogFragment.presentError(getFragmentManager(), error);
    }

    @Override
    public void onPlaybackPulse(@NonNull SoundPlayer player, int position) {
        soundSeekBar.setProgress(position);
    }

    //endregion
}
