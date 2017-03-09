package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

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
import is.hello.sense.bluetooth.exceptions.SenseRequiredException;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.views.SleepSoundsView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.interactors.SleepSoundsStatusInteractor;
import is.hello.sense.mvp.presenters.ControllerPresenterFragment;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.NotTested;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import static is.hello.sense.util.Constants.EMPTY_STRING;

@NotTested
public class SleepSoundsFragment extends ControllerPresenterFragment<SleepSoundsView>
        implements
        SleepSoundsAdapter.InteractionListener,
        SleepSoundsAdapter.Retry,
        HomeActivity.ScrollUp,
        ViewPagerPresenterFragment.FabListener {
    private static final int SOUNDS_REQUEST_CODE = 123;
    private static final int DURATION_REQUEST_CODE = 231;
    private static final int VOLUME_REQUEST_CODE = 312;

    @Inject
    SleepSoundsStatusInteractor sleepSoundsStatusInteractor;

    @Inject
    SleepSoundsInteractor sleepSoundsInteractor;
    @Inject
    PreferencesInteractor preferencesInteractor;

    private Subscription saveOperationSubscriber = Subscriptions.empty();
    private Subscription stopOperationSubscriber = Subscriptions.empty();
    private Subscription hasSensePairedSubscription = Subscriptions.empty();

    private final Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            sleepSoundsStatusInteractor.resetBackOffIfNeeded();
            sleepSoundsInteractor.update();
        }
    };
    public final Runnable getCombinedStateRunnable = new Runnable() {
        @Override
        public void run() {
            sleepSoundsInteractor.update();
        }
    };

    private UserWants userWants = UserWants.NONE;
    private State currentState = State.IDLE;
    @Nullable
    private ViewPagerPresenterFragment.NotificationListener notificationListener = null;

    enum UserWants {
        PLAY,
        STOP,
        NONE
    }


    enum State {
        PLAYING,
        IDLE,
        LOADING,
        ERROR,
        UPDATING
    }

    //region PresenterFragment

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(sleepSoundsInteractor);
        addInteractor(sleepSoundsStatusInteractor);
    }

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new SleepSoundsView(getActivity(),
                                                     new SleepSoundsAdapter(
                                                             getActivity(),
                                                             preferencesInteractor,
                                                             this,
                                                             getAnimatorContext(),
                                                             this)
            );
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsStatusInteractor.state, this::bindStatus, this::presentStatusError);
        bindAndSubscribe(sleepSoundsInteractor.combinedState, this::bind, this::presentError);
        bindAndSubscribe(preferencesInteractor.observeChangesOn(PreferencesInteractor.SLEEP_SOUNDS_SOUND_ID,
                                                                PreferencesInteractor.SLEEP_SOUNDS_VOLUME_ID,
                                                                PreferencesInteractor.SLEEP_SOUNDS_DURATION_ID),
                         changedKey -> {
                             if (presenterView != null) {
                                 presenterView.notifyAdapter();
                             }
                         },
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.notificationListener = null;
        updateSensePairedSubscription(null);
    }

    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        if (isVisible) {
            Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS, null);
            if (currentState == State.IDLE) {
                if (presenterView.isShowingPlayer()) {
                    displayPlayButton();
                } else {
                    displayLoadingButton();
                }
            }
            updateSensePairedSubscription(getCombinedStateRunnable::run);
        } else {
            sleepSoundsStatusInteractor.stopPolling();
        }
    }


    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final int value = data.getIntExtra(ListActivity.VALUE_ID, Constants.NONE);
            if (value == Constants.NONE) {
                return;
            }
            final String constant;
            switch (requestCode) {
                case SOUNDS_REQUEST_CODE:
                    constant = PreferencesInteractor.SLEEP_SOUNDS_SOUND_ID;
                    break;
                case DURATION_REQUEST_CODE:
                    constant = PreferencesInteractor.SLEEP_SOUNDS_DURATION_ID;
                    break;
                case VOLUME_REQUEST_CODE:
                    constant = PreferencesInteractor.SLEEP_SOUNDS_VOLUME_ID;
                    break;
                default:
                    constant = EMPTY_STRING;
            }
            if (EMPTY_STRING.equals(constant)) {
                return;
            }
            preferencesInteractor.edit()
                                 .putInt(constant, value)
                                 .apply();
        }
    }

    //endregion

    //region InteractionListener
    @Override
    public void onSoundClick(final int currentSound,
                             @NonNull final SleepSounds sleepSounds) {
        ListActivity.startActivityForResult(
                this,
                SOUNDS_REQUEST_CODE,
                R.string.list_activity_sound_title,
                currentSound,
                sleepSounds,
                true);
    }

    @Override
    public void onDurationClick(final int currentDuration,
                                @NonNull final SleepDurations sleepDurations) {
        ListActivity.startActivityForResult(
                this,
                DURATION_REQUEST_CODE,
                R.string.list_activity_duration_title,
                currentDuration,
                sleepDurations,
                false);
    }

    @Override
    public void onVolumeClick(final int currentVolume,
                              @NonNull final SleepSoundStatus status) {
        ListActivity.startActivityForResult(
                this,
                VOLUME_REQUEST_CODE,
                R.string.list_activity_volume_title,
                currentVolume,
                status,
                false);
    }
    //endRegion

    //region Retry
    @Override
    public void retry() {
        presenterView.setProgressBarVisible(true);
        updateSensePairedSubscription(retryRunnable::run);
    }
    //endregion

    //region ScrollUp
    @Override
    public void scrollUp() {
        if (presenterView == null) {
            return;
        }
        presenterView.scrollUp();
    }

    //endregion

    //region FabListener
    @Override
    public void onFabClick() {
        if (currentState == State.IDLE) {
            onPlayClickListener();
        } else if (currentState == State.PLAYING) {
            onStopClickListener();
        }
    }

    @Override
    public boolean shouldShowFab() {
        return currentState != State.ERROR && currentState != State.UPDATING;
    }

    @Override
    public int getFabDrawableRes() {
        if (currentState == State.IDLE) {
            return R.drawable.sound_play_icon;
        } else if (currentState == State.PLAYING) {
            return R.drawable.sound_stop_icon;
        }
        return R.drawable.sound_loading_icon;
    }

    @Override
    public void setNotificationListener(@NonNull final ViewPagerPresenterFragment.NotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    @Override
    public boolean hasNotificationListener() {
        return notificationListener != null;
    }

    @Override
    public boolean shouldFabRotate() {
        return currentState == State.LOADING;
    }

    //endregion

    //region FabPresenter helpers
    public void adapterSetState(final SleepSoundsAdapter.AdapterState state) {
        if (state == SleepSoundsAdapter.AdapterState.FIRMWARE_UPDATE
                || state == SleepSoundsAdapter.AdapterState.SOUNDS_DOWNLOAD
                || state == SleepSoundsAdapter.AdapterState.OFFLINE) {
            this.currentState = State.UPDATING;
            this.sleepSoundsStatusInteractor.stopPolling();
        } else {
            displayPlayButton();
        }
        presenterView.adapterSetState(state);
        this.notifyChange();
    }

    private void notifyChange() {
        if (this.notificationListener != null) {
            this.notificationListener.notifyChange();
        }
    }

    private void displayPlayButton() {
        final State previousState = currentState;
        currentState = State.IDLE;
        if (previousState != State.ERROR) {
            notifyChange();
        }
    }

    public void displayStopButton() {
        final State previousState = currentState;
        currentState = State.PLAYING;
        if (previousState != State.ERROR) {
            notifyChange();
        }
    }

    public void displayLoadingButton() {
        final State previousState = currentState;
        currentState = State.LOADING;
        if (previousState != State.ERROR) {
            notifyChange();
        }
    }

    //endregion

    //region methods
    private void updateSensePairedSubscription(@Nullable final Action0 onNextAction) {

        this.hasSensePairedSubscription.unsubscribe();
        if (onNextAction == null) {
            return;
        }
        this.hasSensePairedSubscription =
                bind(sleepSoundsInteractor.hasSensePaired())
                        .subscribe(ignore -> {
                            onNextAction.call();
                        }, this::presentError);
    }

    private void bindStatus(final @NonNull SleepSoundStatus status) {
        presenterView.setProgressBarVisible(false);
        if (presenterView.isShowingPlayer()) {
            if (status.isPlaying()) {
                if (userWants != UserWants.STOP) {
                    displayStopButton();
                    userWants = UserWants.NONE;
                } else if (sleepSoundsStatusInteractor.isTimedOut()) {
                    presentCommandError(null);
                    displayStopButton();
                    userWants = UserWants.NONE;
                }
            } else {
                if (userWants != UserWants.PLAY) {
                    displayPlayButton();
                    userWants = UserWants.NONE;
                } else if (sleepSoundsStatusInteractor.isTimedOut()) {
                    presentCommandError(null);
                    displayPlayButton();
                    userWants = UserWants.NONE;
                }
            }
        } else {
            displayPlayButton();
        }
        presenterView.adapterBindStatus(status);
        if (sleepSoundsStatusInteractor.resetBackOffIfNeeded()) {
            sleepSoundsStatusInteractor.startPolling();
        }
    }

    private void bind(final @NonNull SleepSoundsStateDevice stateDevice) {
        this.presenterView.setProgressBarVisible(false);
        final Devices devices = stateDevice.getDevices();
        final SleepSoundsState combinedState = stateDevice.getSleepSoundsState();
        if (devices == null || combinedState == null || devices.getSense() == null) {
            // Error. Should never happen (Parent fragment should be hiding this fragment if it does).
            adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
            return;
        }

        if (devices.getSense().getMinutesSinceLastUpdated() >= SleepSoundsStatusInteractor.OFFLINE_MINUTES) {
            // Sense Offline error.
            adapterSetState(SleepSoundsAdapter.AdapterState.OFFLINE);
            return;
        }

        if (combinedState.getSounds() != null) {
            //setState download sounds if combined state sounds is empty
            if (combinedState.getSounds().getSounds().isEmpty()) {
                adapterSetState(SleepSoundsAdapter.AdapterState.SOUNDS_DOWNLOAD);
                return;
            }


            final SleepSounds.State currentState = combinedState.getSounds().getState();
            switch (currentState) {
                case SENSE_UPDATE_REQUIRED:
                    adapterSetState(SleepSoundsAdapter.AdapterState.FIRMWARE_UPDATE);
                    return;
                case SOUNDS_NOT_DOWNLOADED:
                    adapterSetState(SleepSoundsAdapter.AdapterState.SOUNDS_DOWNLOAD);
                    return;
                case OK:
                    sleepSoundsStatusInteractor.startPolling();
                    presenterView.adapterBindState(combinedState);
                    displayLoadingButton();
                    return;
                default:
                    adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
                    return;
            }
        }
        adapterSetState(SleepSoundsAdapter.AdapterState.NONE);

    }


    private void presentCommandError(@Nullable final Throwable error) {
        stopOperationSubscriber.unsubscribe();
        saveOperationSubscriber.unsubscribe();
        userWants = UserWants.NONE;
        //  sleepSoundsStatusInteractor.startAndUpdate();
        final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(error);
        builder.withMessage(StringRef.from(R.string.sleep_sounds_error_commanding));
        showErrorDialog(builder);
    }

    private void presentStatusError(@NonNull final Throwable error) {
        if (error instanceof SleepSoundsStatusInteractor.ObservableEndThrowable) {
            //do nothing
            return;
        }
        sleepSoundsStatusInteractor.incrementBackoff();
        if (userWants == UserWants.NONE) {
            presenterView.setProgressBarVisible(false);
        }
        if (sleepSoundsStatusInteractor.isBackOffMaxed()) {
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(error);
            builder.withMessage(StringRef.from(R.string.sleep_sounds_error_communicating_with_sense));
            showErrorDialog(builder);
            userWants = UserWants.NONE;
            sleepSoundsStatusInteractor.stopPolling();
            displayPlayButton();
        } else {
            // Call this to use the new back off interval
            sleepSoundsStatusInteractor.startPolling();
        }
    }

    private void presentError(@NonNull final Throwable error) {
        presenterView.setProgressBarVisible(false);
        if (error instanceof SenseRequiredException) {
            adapterSetState(SleepSoundsAdapter.AdapterState.SENSE_NOT_PAIRED);
        } else {
            adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
        }
        this.currentState = State.ERROR;
        notifyChange();
    }


    private void onPlayClickListener() {
        displayLoadingButton();
        userWants = UserWants.PLAY;
        if (!presenterView.isShowingPlayer()) {
            displayPlayButton();
            return;
        }

        final Sound sound = presenterView.getDisplayedSound();
        final Duration duration = presenterView.getDisplayedDuration();
        final SleepSoundStatus.Volume volume = presenterView.getDisplayedVolume();
        if (sound == null || duration == null || volume == null) {
            displayPlayButton();
            return;
        }

        // send a play event no matter and track errors, if any, from the request
        Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS_PLAY,
                             Analytics.createProperties(Analytics.SleepSounds.PROP_SLEEP_SOUNDS_SOUND_ID, sound.getId(),
                                                        Analytics.SleepSounds.PROP_SLEEP_SOUNDS_DURATION_ID, duration.getId(),
                                                        Analytics.SleepSounds.PROP_SLEEP_SOUNDS_VOLUME, volume.getVolume()));

        final Observable<VoidResponse> saveOperation = sleepSoundsInteractor.play(new SleepSoundActionPlay(sound.getId(),
                                                                                                           duration.getId(),
                                                                                                           volume.getVolume()));
        sleepSoundsStatusInteractor.resetTimeSent();
        saveOperationSubscriber.unsubscribe();
        saveOperationSubscriber = saveOperation.subscribe(voidResponse -> {
                                                              // incase we were in a failed state
                                                              if (isVisibleToUserAndResumed()) {
                                                                  sleepSoundsStatusInteractor.startPolling();
                                                              }
                                                              saveOperationSubscriber.unsubscribe();
                                                          },
                                                          e -> {
                                                              // Failed to send
                                                              presentCommandError(e);
                                                              displayPlayButton();
                                                          });
    }

    private void onStopClickListener() {
        displayLoadingButton();
        userWants = UserWants.STOP;
        Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS_STOP, null);

        final Observable<VoidResponse> saveOperation = sleepSoundsInteractor.stop(new SleepSoundActionStop());
        sleepSoundsStatusInteractor.resetTimeSent();
        stopOperationSubscriber.unsubscribe();
        stopOperationSubscriber = saveOperation.subscribe(voidResponse -> {
                                                              // incase we were in a failed state
                                                              if (isVisibleToUserAndResumed()) {
                                                                  sleepSoundsStatusInteractor.startPolling();
                                                              }
                                                              stopOperationSubscriber.unsubscribe();
                                                          },
                                                          e -> {
                                                              presentCommandError(e);
                                                              displayStopButton();
                                                          });
    }

    private boolean isVisibleToUserAndResumed() {
        return this.isVisibleToUser() && isResumed();
    }
    //endregion

}
