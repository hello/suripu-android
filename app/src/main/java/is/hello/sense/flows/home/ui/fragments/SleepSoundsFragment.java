package is.hello.sense.flows.home.ui.fragments;

import android.content.Context;
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
import is.hello.sense.flows.home.ui.views.SleepSoundsView;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.interactors.SleepSoundsStatusInteractor;
import is.hello.sense.mvp.presenters.SubPresenterFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.NotTested;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@NotTested
public class SleepSoundsFragment extends SubPresenterFragment<SleepSoundsView>
        implements
        SleepSoundsAdapter.InteractionListener,
        SleepSoundsAdapter.Retry {
    private static final int SOUNDS_REQUEST_CODE = 123;
    private static final int DURATION_REQUEST_CODE = 231;
    private static final int VOLUME_REQUEST_CODE = 312;

    @Inject
    SleepSoundsStatusInteractor sleepSoundsStatusInteractor;

    @Inject
    SleepSoundsInteractor sleepSoundsInteractor;

    private Subscription saveOperationSubscriber = Subscriptions.empty();
    private Subscription stopOperationSubscriber = Subscriptions.empty();


    private UserWants userWants = UserWants.NONE;

    enum UserWants {
        PLAY,
        STOP,
        NONE
    }
    //region PresenterFragment

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(sleepSoundsInteractor);
        addInteractor(sleepSoundsStatusInteractor);
        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.SleepSounds.EVENT_SLEEP_SOUNDS, null);
        }
    }

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new SleepSoundsView(getActivity(),
                                                     new SleepSoundsAdapter(
                                                             getActivity(),
                                                             getActivity().getSharedPreferences(Constants.SLEEP_SOUNDS_PREFS, Context.MODE_PRIVATE),
                                                             this,
                                                             getAnimatorContext(),
                                                             this),
                                                     this::onPlayClickListener,
                                                     this::onStopClickListener);
        }
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsStatusInteractor.state, this::bindStatus, this::presentStatusError);
        bindAndSubscribe(sleepSoundsInteractor.sub, this::bind, this::presentError);
        sleepSoundsInteractor.update();
    }

    @Override
    public void onUserVisible() {
        final boolean flickerWorkAround = true;
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_sleep_sounds, flickerWorkAround);
        sleepSoundsStatusInteractor.resetBackOffIfNeeded();
        sleepSoundsStatusInteractor.startPolling();
    }

    @Override
    public void onUserInvisible() {
        sleepSoundsStatusInteractor.stopPolling();
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

    }
    //endregion

    //region methods

    private void bindStatus(final @NonNull SleepSoundStatus status) {
        presenterView.setProgressBarVisible(false);
        if (presenterView.isShowingPlayer()) {
            presenterView.setButtonVisible(true);
            if (status.isPlaying()) {
                if (userWants != UserWants.STOP) {
                    presenterView.displayStopButton();
                    userWants = UserWants.NONE;
                } else if (sleepSoundsStatusInteractor.isTimedOut()) {
                    presentCommandError(null);
                    presenterView.displayStopButton();
                    userWants = UserWants.NONE;
                }
            } else {
                if (userWants != UserWants.PLAY) {
                    presenterView.displayPlayButton();
                    userWants = UserWants.NONE;
                } else if (sleepSoundsStatusInteractor.isTimedOut()) {
                    presentCommandError(null);
                    presenterView.displayPlayButton();
                    userWants = UserWants.NONE;
                }
            }
        } else {
            presenterView.setButtonVisible(false);
        }
        presenterView.adapterBindStatus(status);
        sleepSoundsStatusInteractor.resetBackOffIfNeeded();
    }

    private void bind(final @NonNull SleepSoundsStateDevice stateDevice) {
        final Devices devices = stateDevice.getDevices();
        final SleepSoundsState combinedState = stateDevice.getSleepSoundsState();
        if (devices == null || combinedState == null || devices.getSense() == null) {
            // Error. Should never happen (Parent fragment should be hiding this fragment if it does).
            presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
            return;
        }

        if (devices.getSense().getMinutesSinceLastUpdated() >= SleepSoundsStatusInteractor.OFFLINE_MINUTES) {
            // Sense Offline error.
            presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.OFFLINE);
            return;
        }

        if (combinedState.getSounds() != null) {
            //setState download sounds if combined state sounds is empty
            if (combinedState.getSounds().getSounds().isEmpty()) {
                presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.SOUNDS_DOWNLOAD);
                return;
            }


            final SleepSounds.State currentState = combinedState.getSounds().getState();
            switch (currentState) {
                case SENSE_UPDATE_REQUIRED:
                    presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.FIRMWARE_UPDATE);
                    return;
                case SOUNDS_NOT_DOWNLOADED:
                    presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.SOUNDS_DOWNLOAD);
                    return;
                case OK:
                    presenterView.adapterBindState(combinedState);
                    presenterView.displayLoadingButton();
                    presenterView.setButtonVisible(true);
                    return;
                default:
                    presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
                    return;
            }
        }
        presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.NONE);

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
            presenterView.displayPlayButton();
        } else {
            // Call this to use the new back off interval
            sleepSoundsStatusInteractor.startPolling();
        }
    }

    private void presentError(@NonNull final Throwable error) {
        presenterView.adapterSetState(SleepSoundsAdapter.AdapterState.ERROR);
        presenterView.setProgressBarVisible(false);
    }


    private void onPlayClickListener(@NonNull final View ignored) {
        presenterView.displayLoadingButton();
        userWants = UserWants.PLAY;
        if (!presenterView.isShowingPlayer()) {
            presenterView.displayPlayButton();
            return;
        }

        final Sound sound = presenterView.getDisplayedSound();
        final Duration duration = presenterView.getDisplayedDuration();
        final SleepSoundStatus.Volume volume = presenterView.getDisplayedVolume();
        if (sound == null || duration == null || volume == null) {
            presenterView.displayPlayButton();
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
                                                              presenterView.displayPlayButton();
                                                          });
    }

    private void onStopClickListener(@NonNull final View ignored) {
        presenterView.displayLoadingButton();
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
                                                              presenterView.displayStopButton();
                                                          });
        ;
    }

    //endregion

}
