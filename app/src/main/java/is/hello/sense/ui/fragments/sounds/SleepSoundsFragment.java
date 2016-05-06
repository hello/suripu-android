package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;



import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundActionPlay;
import is.hello.sense.api.model.v2.SleepSoundActionStop;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.graph.presenters.SleepSoundsPresenter;
import is.hello.sense.graph.presenters.SleepSoundsStatusPresenter;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import rx.Observable;

import static is.hello.sense.ui.adapter.SleepSoundsAdapter.*;

public class SleepSoundsFragment extends SubFragment implements InteractionListener, Retry {
    private final static int pollingInterval = 500; // ms
    private final static int initialBackOff = 0; // ms
    private final static int backOffIncrements = 1000; // ms
    private final static int maxBackOff = 6000; //ms
    private final static int offlineMinutes = 30; // minutes
    private static final int SOUNDS_REQUEST_CODE = 123;
    private static final int DURATION_REQUEST_CODE = 231;
    private static final int VOLUME_REQUEST_CODE = 312;


    @Inject
    SleepSoundsStatusPresenter sleepSoundsStatusPresenter;

    @Inject
    SleepSoundsPresenter sleepSoundsPresenter;


    private SpinnerImageView playButton;
    private FrameLayout buttonLayout;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private SleepSoundsAdapter adapter;
    private SharedPreferences preferences;
    private UserWants userWants = UserWants.NONE;
    private final StatusPollingHelper statusPollingHelper = new StatusPollingHelper();
    private int backOff = initialBackOff;
    private long timeSent = 0;

    @Override
    public void retry() {
        sleepSoundsPresenter.update();
        progressBar.setVisibility(View.VISIBLE);
    }

    enum UserWants {
        PLAY,
        STOP,
        NONE
    }

    final View.OnClickListener playClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayLoadingButton(UserWants.PLAY);
            if (!adapter.isShowingPlayer()) {
                displayPlayButton();
                return;
            }

            final Sound sound = adapter.getDisplayedSound();
            final Duration duration = adapter.getDisplayedDuration();
            final SleepSoundStatus.Volume volume = adapter.getDisplayedVolume();
            if (sound == null || duration == null || volume == null) {
                displayPlayButton();
                return;
            }

            // send a play event no matter and track errors, if any, from the request
            Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS_PLAY,
                                 Analytics.createProperties(Analytics.SleepSounds.PROP_SLEEP_SOUDNS_SOUND_ID, sound.getId(),
                                                            Analytics.SleepSounds.PROP_SLEEP_SOUNDS_DURATION_ID, duration.getId(),
                                                            Analytics.SleepSounds.PROP_SLEEP_SOUNDS_VOLUME, volume.getVolume()));

            final Observable<VoidResponse> saveOperation = sleepSoundsPresenter.play(new SleepSoundActionPlay(sound.getId(), duration.getId(), volume.getVolume()));
            timeSent = System.currentTimeMillis();
            bindAndSubscribe(saveOperation,
                             ignored -> {
                                 // incase we were in a failed state
                                 statusPollingHelper.poll();
                             },
                             e -> {
                                 // Failed to send
                                 presentCommandError(e);
                                 displayPlayButton();
                             });
        }
    };

    final View.OnClickListener stopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayLoadingButton(UserWants.STOP);

            Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS_STOP, null);

            final Observable<VoidResponse> saveOperation = sleepSoundsPresenter.stop(new SleepSoundActionStop());
            timeSent = System.currentTimeMillis();
            bindAndSubscribe(saveOperation,
                             ignored -> {
                                 // incase we were in a failed state
                                 statusPollingHelper.poll();
                             },
                             SleepSoundsFragment.this::presentCommandError);
        }
    };


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        statusPollingHelper.setViewVisible(isVisibleToUser);
        // This method is called before onCreateView, when not visible to the user.
        // When it is visible, then this is called after the view has been created,
        // although we really do not depend on the view being created first.
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS, null);
            update();
            if (getActivity() != null) {
                final boolean flickerWorkAround = true;
                WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_sleep_sounds, flickerWorkAround);
            }
            statusPollingHelper.poll();
        } else {
            statusPollingHelper.cancelLastPoll();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sleep_sounds, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.fragment_sleep_sounds_progressbar);
        playButton = (SpinnerImageView) view.findViewById(R.id.fragment_sleep_sounds_playbutton);
        buttonLayout = (FrameLayout) view.findViewById(R.id.fragment_sleep_sounds_buttonLayout);
        preferences = getActivity().getSharedPreferences(Constants.SLEEP_SOUNDS_PREFS, 0);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_sleep_sounds_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        this.adapter = new SleepSoundsAdapter(getActivity(), preferences, this, getAnimatorContext(), this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsStatusPresenter.state, this::bindStatus, this::presentStatusError);
        bindAndSubscribe(sleepSoundsPresenter.sub, this::bind, this::presentError);
        update();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        statusPollingHelper.cancelLastPoll();
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
            final int value = data.getIntExtra(ListActivity.VALUE_ID, -1);
            if (value == -1) {
                return;
            }
            final String constant;
            if (requestCode == SOUNDS_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_SOUND_ID;
            } else if (requestCode == DURATION_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_DURATION_ID;
            } else {
                constant = Constants.SLEEP_SOUNDS_VOLUME_ID;
            }
            preferences.edit()
                       .putInt(constant, value)
                       .apply();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void update() {
        sleepSoundsStatusPresenter.update();
        sleepSoundsPresenter.update();
    }

    private void bind(final @NonNull SleepSoundsStateDevice stateDevice) {
        final Devices devices = stateDevice.getDevices();
        final SleepSoundsState state = stateDevice.getSleepSoundsState();
        if (devices == null || state == null || devices.getSense() == null) {
            // Error. Should never happen (Parent fragment should be hiding this fragment if it does).
            adapter.setErrorState();
            setButtonVisible(false);
            return;
        }

        if (devices.getSense().getMinutesSinceLastUpdated() >= offlineMinutes) {
            // Sense Offline error.
            adapter.setOfflineTooLong();
            setButtonVisible(false);
            return;
        }

        adapter.bindData(state);
        if (state.getSounds() != null && state.getSounds().getState() == SleepSounds.State.OK) {
            displayLoadingButton(userWants);
            setButtonVisible(true);
            // Show.
            return;
        }

        if (state.getSounds() !=null){
            if (state.getSounds().getState() == SleepSounds.State.SENSE_UPDATE_REQUIRED){
                adapter.setUpdatingState();
                setButtonVisible(false);
                return;
            }
        }

        setButtonVisible(false);
        adapter.setErrorState();
    }

    public void bindStatus(final @NonNull SleepSoundStatus status) {

        progressBar.setVisibility(View.GONE);
        if (adapter.isShowingPlayer()) {
            setButtonVisible(true);
            if (status.isPlaying()) {
                if (userWants != UserWants.STOP) {
                    displayStopButton();
                } else if (System.currentTimeMillis() - timeSent > 30000) {
                    presentCommandError(null);
                    displayStopButton();
                    userWants = UserWants.NONE;
                }
            } else {
                if (userWants != UserWants.PLAY) {
                    displayPlayButton();
                } else if (System.currentTimeMillis() - timeSent > 30000) {
                    presentCommandError(null);
                    displayPlayButton();
                }
            }
        } else {
            setButtonVisible(false);
        }
        adapter.bind(status);
        backOff = initialBackOff;
        statusPollingHelper.poll();
    }

    private void presentError(final @NonNull Throwable error) {
        adapter.setErrorState();
        setButtonVisible(false);
        progressBar.setVisibility(View.GONE);
    }

    private void presentStatusError(final @NonNull Throwable error) {
        backOff += backOffIncrements;
        if (backOff > maxBackOff) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(error, getResources())
                    .withMessage(StringRef.from(R.string.sleep_sounds_error_communicating_with_sense))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            return; // give up
        }
        statusPollingHelper.poll();
    }


    private void presentCommandError(final @Nullable Throwable error) {
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(error, getResources())
                .withMessage(StringRef.from(R.string.sleep_sounds_error_commanding))
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

    }

    private void setButtonVisible(final boolean visible) {
        if (visible) {
            buttonLayout.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.VISIBLE);
        } else {
            buttonLayout.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
        }
    }

    private void displayButton(final @DrawableRes int resource,
                               final @Nullable View.OnClickListener listener,
                               final boolean enabled,
                               final @NonNull UserWants wants) {
        userWants = wants;
        buttonLayout.setVisibility(View.VISIBLE);
        playButton.setRotation(0);
        playButton.setImageResource(resource);
        playButton.setOnClickListener(listener);
        playButton.setEnabled(enabled);
        if (enabled) {
            playButton.stopSpinning();
        } else {
            playButton.startSpinning();
        }
    }

    private void displayPlayButton() {
        displayButton(R.drawable.sound_play_icon, playClickListener, true, UserWants.NONE);
    }

    private void displayStopButton() {
        displayButton(R.drawable.sound_stop_icon, stopClickListener, true, UserWants.NONE);
    }

    private void displayLoadingButton(UserWants userWants) {
        displayButton(R.drawable.sound_loading_icon, null, false, userWants);
    }


    @Override
    public void onSoundClick(final int currentSound, final @NonNull SleepSounds sleepSounds) {
        ListActivity.startActivityForResult(
                this,
                SOUNDS_REQUEST_CODE,
                R.string.list_activity_sound_title,
                currentSound,
                sleepSounds,
                true);
    }

    @Override
    public void onDurationClick(final int currentDuration, final @NonNull SleepDurations durations) {
        ListActivity.startActivityForResult(
                this,
                DURATION_REQUEST_CODE,
                R.string.list_activity_duration_title,
                currentDuration,
                durations,
                false);
    }

    @Override
    public void onVolumeClick(final int currentVolume, @NonNull SleepSoundStatus volumes) {

        ListActivity.startActivityForResult(
                this,
                VOLUME_REQUEST_CODE,
                R.string.list_activity_volume_title,
                currentVolume,
                volumes,
                false);
    }

    private class StatusPollingHelper {
        private boolean isViewVisible;
        private boolean isRunning = false;
        private final Handler statusPollingHandler = new Handler();

        public void setViewVisible(boolean isViewVisible) {
            this.isViewVisible = isViewVisible;
        }

        final Runnable statusPollingRunnable = new Runnable() {
            @Override
            public void run() {
                isRunning = false;
                sleepSoundsStatusPresenter.update();
            }
        };

        private void poll() {
            if (!isRunning && isViewVisible) {
                isRunning = true;
                statusPollingHandler.postDelayed(statusPollingRunnable, pollingInterval + backOff);
            }
        }

        private void cancelLastPoll() {
            statusPollingHandler.removeCallbacks(statusPollingRunnable);
            isRunning = false;
        }
    }

}
