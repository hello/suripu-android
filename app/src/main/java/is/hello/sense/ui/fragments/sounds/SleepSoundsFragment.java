package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.graph.presenters.SleepSoundsStatePresenter;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.util.Constants;

public class SleepSoundsFragment extends InjectionFragment implements SleepSoundsAdapter.InteractionListener {
    private final static int SOUNDS_REQUEST_CODE = 1234;
    private final static int DURATION_REQUEST_CODE = 4321;

    private ProgressBar progressBar;
    private View soundsHolder;
    private View durationsHolder;
    private TextView soundsValue;
    private TextView durationsValue;
    private ImageButton playButton;

    private SleepSounds currentSounds;
    private SleepDurations currentDurations;
    private SleepSoundStatus currentStatus;
    private SharedPreferences preferences;

    private static final String ARG_STATUS = SleepSoundsFragment.class.getName() + ".ARG_STATUS";
    private static final String ARG_SOUNDS = SleepSoundsFragment.class.getName() + ".ARG_SOUNDS";
    private static final String ARG_DURATIONS = SleepSoundsFragment.class.getName() + ".ARG_DURATIONS";

    public static final String VALUE_NAME = SleepSoundsFragment.class.getName() + ".VALUE_NAME";

    @Inject
    SleepSoundsStatePresenter sleepSoundsStatePresenter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sleep_sounds, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.fragment_sleep_sounds_progressbar);
        soundsHolder = view.findViewById(R.id.fragment_sleep_sounds_sound_holder);
        durationsHolder = view.findViewById(R.id.fragment_sleep_sounds_duration_holder);
        soundsValue = (TextView) view.findViewById(R.id.fragment_sleep_sounds_sound_value);
        durationsValue = (TextView) view.findViewById(R.id.fragment_sleep_sounds_duration_value);
        playButton = (ImageButton) view.findViewById(R.id.fragment_sleep_sounds_playbutton);
        preferences = getActivity().getSharedPreferences(Constants.SLEEP_SOUNDS_PREFS, 0);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_STATUS)) {
                currentStatus = (SleepSoundStatus) savedInstanceState.getSerializable(ARG_STATUS);
            }
            if (savedInstanceState.containsKey(ARG_SOUNDS)) {
                currentSounds = (SleepSounds) savedInstanceState.getSerializable(ARG_SOUNDS);
            }
            if (savedInstanceState.containsKey(ARG_DURATIONS)) {
                currentDurations = (SleepDurations) savedInstanceState.getSerializable(ARG_DURATIONS);
            }
            progressBar.setVisibility(View.GONE);
        }
        bindAndSubscribe(sleepSoundsStatePresenter.state, this::bind, this::presentError);
        sleepSoundsStatePresenter.update();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_STATUS, currentStatus);
        outState.putSerializable(ARG_SOUNDS, currentSounds);
        outState.putSerializable(ARG_DURATIONS, currentDurations);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String value = data.getStringExtra(ListActivity.VALUE_NAME);
            if (value == null) {
                return;
            }
            final String constant;
            if (requestCode == SOUNDS_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_SOUND_NAME;
            } else {
                constant = Constants.SLEEP_SOUNDS_DURATION_NAME;
            }
            preferences.edit()
                       .putString(constant, value)
                       .apply();
            refreshUI();
        }
    }

    private void presentError(@NonNull Throwable error) {
    }

    private void bind(@NonNull SleepSoundsState state) {
        progressBar.setVisibility(View.GONE);
        this.currentStatus = state.getStatus();
        this.currentDurations = state.getDurations();
        this.currentSounds = state.getSounds();
        refreshUI();
    }

    private void refreshUI() {
        refreshSounds();
        refreshDurations();
        refreshStatus(currentStatus);
    }

    private void refreshSounds() {
        if (currentSounds == null) {
            return;
        }

        List<Sound> sounds = currentSounds.getSounds();

        if (sounds.isEmpty()) {
            soundsValue.setText(null);
            return;
        }

        soundsHolder.setOnClickListener(v -> {
            ListActivity.startActivityForResult(
                    this,
                    SOUNDS_REQUEST_CODE,
                    soundsValue.getText().toString(),
                    sounds);
        });

        String savedSound = getSavedSound();
        if (currentSounds.hasSound(savedSound)) {
            soundsValue.setText(savedSound);
        } else if (currentSounds.hasSound(currentStatus.getSound().getName())) {
            soundsValue.setText(currentStatus.getSound().getName());
        } else {
            soundsValue.setText(sounds.get(0).getName());
        }
    }

    private void refreshDurations() {

        if (currentDurations == null) {
            return;
        }

        List<Duration> durations = currentDurations.getDurations();

        if (durations.isEmpty()) {
            durationsValue.setText(null);
            return;
        }

        durationsHolder.setOnClickListener(v -> {
            ListActivity.startActivityForResult(
                    this,
                    DURATION_REQUEST_CODE,
                    durationsValue.getText().toString(),
                    durations);
        });

        String savedDuration = getSavedDuration();
        if (currentDurations.hasDuration(savedDuration)) {
            durationsValue.setText(savedDuration);
        } else if (currentDurations.hasDuration(currentStatus.getDuration().getName())) {
            durationsValue.setText(currentStatus.getDuration().getName());
        } else {
            durationsValue.setText(durations.get(0).getName());
        }

    }

    private void refreshStatus(@NonNull SleepSoundStatus status) {
        if (status.isPlaying()) {
            playButton.setImageResource(R.drawable.timeline_pause);
        } else {
            playButton.setImageResource(R.drawable.timeline_play);
        }
    }

    private String getSavedSound() {
        return preferences.getString(Constants.SLEEP_SOUNDS_SOUND_NAME, null);
    }

    private String getSavedDuration() {
        return preferences.getString(Constants.SLEEP_SOUNDS_DURATION_NAME, null);
    }

    @Override
    public void onSoundClick(@NonNull String currentValue, @NonNull List<?> sounds) {
        ListActivity.startActivityForResult(
                this,
                SOUNDS_REQUEST_CODE,
                currentValue,
                sounds);
    }

    @Override
    public void onDurationClick(@NonNull String currentDuration, @NonNull List<?> durations) {
        ListActivity.startActivityForResult(
                this,
                DURATION_REQUEST_CODE,
                currentDuration,
                durations);
    }
}
