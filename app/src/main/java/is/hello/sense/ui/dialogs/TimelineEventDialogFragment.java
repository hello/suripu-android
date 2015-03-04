package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

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
import rx.functions.Action1;

import static android.view.ViewGroup.MarginLayoutParams;

public final class TimelineEventDialogFragment extends InjectionDialogFragment implements SeekBar.OnSeekBarChangeListener, SoundPlayer.OnEventListener {
    public static final String TAG = TimelineEventDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineEventDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";
    private static final int REQUEST_CODE_ADJUST_TIME = 0x12;

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

    private Button adjustTimeButton;
    private ProgressBar adjustTimeActivity;

    private boolean adjustingTime = false;


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

        if (timelineSegment.isTimeAdjustable()) {
            ViewGroup adjustContainer = (ViewGroup) dialog.findViewById(R.id.dialog_fragment_timeline_event_adjust_time_container);
            adjustContainer.setVisibility(View.VISIBLE);

            this.adjustTimeActivity = (ProgressBar) adjustContainer.findViewById(R.id.dialog_fragment_timeline_event_adjust_time_activity);
            this.adjustTimeButton = (Button) adjustContainer.findViewById(R.id.dialog_fragment_timeline_event_adjust_time_button);
            Views.setSafeOnClickListener(adjustTimeButton, this::adjustSegmentTime);

            if (adjustingTime) {
                adjustTimeActivity.setVisibility(View.VISIBLE);
                adjustTimeButton.setVisibility(View.INVISIBLE);
            }

            MarginLayoutParams layoutParams = (MarginLayoutParams) message.getLayoutParams();
            layoutParams.bottomMargin = 0;
        }

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

    private void setTimelineSegment(@NonNull TimelineSegment newSegment) {
        this.timelineSegment = newSegment;
        getArguments().putSerializable(ARG_SEGMENT, newSegment);

        bindTimelineSegment();
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


    //region Adjust Time

    private AdjustTimeFragment getTimeAdjustFragment() {
        return (AdjustTimeFragment) getTargetFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADJUST_TIME && resultCode == Activity.RESULT_OK) {
            int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 12);
            int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 0);
            DateTime newLocalTimestamp = timelineSegment.getShiftedTimestamp()
                                                        .withHourOfDay(hour)
                                                        .withMinuteOfHour(minute);
            LocalTime newTime = newLocalTimestamp.toLocalTime();

            this.adjustingTime = true;
            adjustTimeActivity.setVisibility(View.VISIBLE);
            adjustTimeButton.setVisibility(View.INVISIBLE);

            getTimeAdjustFragment().onAdjustSegmentTime(timelineSegment, newTime, success -> {
                coordinator.postOnResume(() -> {
                    this.adjustingTime = false;

                    adjustTimeActivity.setVisibility(View.GONE);
                    adjustTimeButton.setVisibility(View.VISIBLE);
                    if (success) {
                        setTimelineSegment(timelineSegment.withTimestamp(newLocalTimestamp));
                    }
                });
            });
        }
    }

    public void adjustSegmentTime(@NonNull View sender) {
        LocalTime eventTime = timelineSegment.getShiftedTimestamp().toLocalTime();
        boolean use24Time = preferences.getUse24Time();
        TimePickerDialogFragment dialogFragment = TimePickerDialogFragment.newInstance(eventTime, use24Time);
        dialogFragment.setTargetFragment(this, REQUEST_CODE_ADJUST_TIME);
        dialogFragment.show(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    //endregion


    public interface AdjustTimeFragment {
        void onAdjustSegmentTime(@NonNull TimelineSegment segment,
                                 @NonNull LocalTime newTime,
                                 @NonNull Action1<Boolean> continuation);
    }
}
