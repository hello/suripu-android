package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepSoundActionPlay;
import is.hello.sense.api.model.v2.SleepSoundActionStop;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.graph.presenters.SleepSoundsStatePresenter;
import is.hello.sense.graph.presenters.SleepSoundsStatusPresenter;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.util.Constants;
import rx.Observable;

import static is.hello.sense.ui.adapter.SleepSoundsAdapter.*;

public class SleepSoundsFragment extends InjectionFragment implements InteractionListener {
    private final static int SOUNDS_REQUEST_CODE = 1234;
    private final static int DURATION_REQUEST_CODE = 4321;
    private final static int VOLUME_REQUEST_CODE = 2143;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageButton playButton;
    private SleepSoundsAdapter adapter;
    private SharedPreferences preferences;
    private boolean spin = false;
    private UserWants userWants = UserWants.NONE;

    enum UserWants {
        PLAY,
        STOP,
        NONE
    }

    Runnable buttonSpinner = new Runnable() {
        @Override
        public void run() {
            if (spin && playButton != null) {
                playButton.setRotation(playButton.getRotation() + 5);
                playButton.postDelayed(buttonSpinner, 1);
            }
        }
    };


    @Inject
    SleepSoundsStatePresenter sleepSoundsStatePresenter;

    @Inject
    SleepSoundsStatusPresenter sleepSoundsStatusPresenter;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sleep_sounds, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.fragment_sleep_sounds_progressbar);
        playButton = (ImageButton) view.findViewById(R.id.fragment_sleep_sounds_playbutton);
        preferences = getActivity().getSharedPreferences(Constants.SLEEP_SOUNDS_PREFS, 0);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_sleep_sounds_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        decoration.addBottomInset(3, resources.getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources));
        this.adapter = new SleepSoundsAdapter(getActivity(), preferences, this, getAnimatorContext());
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsStatePresenter.state, this::bindState, this::presentError);
        bindAndSubscribe(sleepSoundsStatusPresenter.state, this::bindStatus, this::presentError);
        sleepSoundsStatePresenter.update();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        recyclerView = null;
        progressBar = null;
        playButton = null;
        adapter = null;
        preferences = null;

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final String value = data.getStringExtra(ListActivity.VALUE_NAME);
            if (value == null) {
                return;
            }
            final String constant;
            if (requestCode == SOUNDS_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_SOUND_NAME;
            } else if (requestCode == DURATION_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_DURATION_NAME;
            } else {
                constant = Constants.SLEEP_SOUNDS_VOLUME_NAME;
            }
            preferences.edit()
                       .putString(constant, value)
                       .apply();
            adapter.notifyDataSetChanged();
        }
    }


    private void bindState(final @NonNull SleepSoundsState state) {
        progressBar.setVisibility(View.GONE);
        adapter.bind(state.getStatus(), state.getSounds(), state.getDurations());
        sleepSoundsStatusPresenter.update();
        playButton.setVisibility(View.VISIBLE);
        displayLoadingButton(userWants);
    }

    public void bindStatus(final @NonNull SleepSoundStatus status) {
        if (status.isPlaying()) {
            if (userWants != UserWants.STOP) {
                displayStopButton();
            }
        } else {
            if (userWants != UserWants.PLAY) {
                displayPlayButton();
            }
        }
        adapter.bind(status);
    }

    private void displayPlayButton() {
        userWants = UserWants.NONE;
        spin = false;
        playButton.setImageResource(R.drawable.sound_play_icon);
        playButton.setRotation(0);

        playButton.setOnClickListener(v -> {
            displayLoadingButton(UserWants.PLAY);
            if (adapter.getItemCount() != 4) {
                displayPlayButton();
                return;
            }

            Sound sound = adapter.getDisplayedSound();
            Duration duration = adapter.getDisplayedDuration();
            SleepSoundStatus.Volume volume = adapter.getDisplayedVolume();
            if (sound == null || duration == null || volume == null) {
                displayPlayButton();
                return;
            }
            final Observable<VoidResponse> saveOperation = sleepSoundsStatePresenter.play(new SleepSoundActionPlay(sound.getId(), duration.getId(), volume.getVolume()));
            bindAndSubscribe(saveOperation, ignored -> {
                                 // do nothing
                             },
                             e -> {
                                 // Failed to send
                                 displayPlayButton();

                             });
        });
        playButton.setEnabled(true);
    }

    private void displayStopButton() {
        userWants = UserWants.NONE;
        spin = false;
        playButton.setImageResource(R.drawable.sound_stop_icon);
        playButton.setRotation(0);

        playButton.setOnClickListener(v -> {
            displayLoadingButton(UserWants.STOP);
            final Observable<VoidResponse> saveOperation = sleepSoundsStatePresenter.stop(new SleepSoundActionStop());
            bindAndSubscribe(saveOperation, ignored -> {
                                 // do nothing
                             },
                             e -> {
                                 // Failed to send
                                 displayStopButton();

                             });
        });
        playButton.setEnabled(true);
    }

    private void displayLoadingButton(UserWants userWants) {
        this.userWants = userWants;
        playButton.setEnabled(false);
        spin = true;
        playButton.post(buttonSpinner);
        playButton.setImageResource(R.drawable.sound_loading_icon);
    }

    private void presentError(final @NonNull Throwable error) {
        Log.e("Updating", "Error");

        //todo report error. Determine how to handle when status fails.
    }

    @Override
    public void onSoundClick(final @NonNull String currentSound, final @NonNull List<?> sounds) {
        ListActivity.startActivityForResult(
                this,
                SOUNDS_REQUEST_CODE,
                R.string.list_activity_sound_title,
                currentSound,
                sounds);
    }

    @Override
    public void onDurationClick(final @NonNull String currentDuration, final @NonNull List<?> durations) {
        ListActivity.startActivityForResult(
                this,
                DURATION_REQUEST_CODE,
                R.string.list_activity_duration_title,
                currentDuration,
                durations);
    }

    @Override
    public void onVolumeClick(@NonNull String currentVolume, @NonNull List<?> volumes) {
        ListActivity.startActivityForResult(
                this,
                VOLUME_REQUEST_CODE,
                R.string.list_activity_volume_title,
                currentVolume,
                volumes);
    }

}
