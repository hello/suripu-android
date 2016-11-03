package is.hello.sense.flows.voice.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.flows.voice.ui.views.VoiceSettingsListView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class VoiceSettingsListFragment extends PresenterFragment<VoiceSettingsListView> {

    @Inject
    VoiceSettingsInteractor settingsInteractor;

    @Inject
    CurrentSenseInteractor currentSenseInteractor;

    public static final int RESULT_VOLUME_SELECTED = 99;
    private static final int RESULT_HELP_PRESSED = 103;
    private Subscription updateSettingsSubscription = Subscriptions.empty();

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new VoiceSettingsListView(getActivity());
            presenterView.setVolumeClickListener(this::redirectToVolumeSelection);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(currentSenseInteractor);
        addInteractor(settingsInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showProgress(true);
        bindAndSubscribe(currentSenseInteractor.senseDevice,
                         this::bindSenseDevice,
                         this::presentError);

        bindAndSubscribe(settingsInteractor.settingsSubject,
                         this::bindSettings,
                         this::presentError);

        currentSenseInteractor.update();
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_HELP_PRESSED && resultCode == RESULT_HELP_PRESSED) {
            UserSupport.showUserGuide(getActivity());
        }
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        updateSettingsSubscription.unsubscribe();
    }

    public void bindSenseDevice(@Nullable final SenseDevice device) {
        settingsInteractor.setSenseId(device == null ? VoiceSettingsInteractor.EMPTY_ID : device.deviceId);
        settingsInteractor.update();
    }

    public void bindSettings(@NonNull final SenseVoiceSettings settings) {
        presenterView.updateVolumeTextView(settings);

        presenterView.updateMuteSwitch(settings.isMuteOrDefault(),
                                       this::onMuteSwitchChanged);

        if (settings.isPrimaryUserOrDefault()) {
            presenterView.makePrimaryUser();
        } else {
            presenterView.makeSecondaryUser(this::showPrimaryUserDialog);
        }
        showProgress(false);
    }

    private void onMuteSwitchChanged(final CompoundButton ignored,
                                     final boolean isMuted) {
        updateSettings(settingsInteractor.setMuted(isMuted));
    }

    private void makePrimaryUser() {
        updateSettings(settingsInteractor.setPrimaryUser(true));
    }

    private void showPrimaryUserDialog(final View ignored) {
        showAlertDialog(new SenseAlertDialog.Builder()
                                .setTitle(R.string.voice_settings_primary_user_dialog_title)
                                .setMessage(R.string.voice_settings_primary_user_dialog_message)
                                .setPositiveButton(R.string.voice_settings_primary_user_dialog_positive_button,
                                                   this::makePrimaryUser));
    }

    private void updateSettings(@NonNull final Observable<SenseVoiceSettings> updateObservable) {
        showBlockingActivity(R.string.voice_settings_progress_updating); //todo use real copy
        updateSettingsSubscription.unsubscribe();
        updateSettingsSubscription = bind(updateObservable)
                .subscribe(Functions.NO_OP,
                           this::presentError,
                           () -> hideBlockingActivity(true, null));

    }

    private void presentError(@NonNull final Throwable e) {
        showProgress(false);
        if (e instanceof VoiceSettingsInteractor.SettingsUpdateThrowable) {
            final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(e);
            builder.withTitle(StringRef.from(R.string.voice_settings_update_error_title));
            builder.withMessage(StringRef.from(R.string.voice_settings_update_error_message));
            builder.withAction(RESULT_HELP_PRESSED, R.string.label_having_trouble);
            showErrorDialog(builder, RESULT_HELP_PRESSED);
            if (e instanceof VoiceSettingsInteractor.MuteUpdateThrowable) {
                presenterView.flipMuteSwitch(this::onMuteSwitchChanged);
            } else if (e instanceof VoiceSettingsInteractor.PrimaryUpdateThrowable) {
                presenterView.makeSecondaryUser(this::showPrimaryUserDialog);
            }
        } else {
            showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
        }
    }

    private void redirectToVolumeSelection(final View ignore) {
        finishFlowWithResult(RESULT_VOLUME_SELECTED);
    }

    private void showProgress(final boolean show) {
        if (show) {
            presenterView.setVisibility(View.INVISIBLE);
            showBlockingActivity(null);
        } else {
            hideBlockingActivity(false, null);
            presenterView.setVisibility(View.VISIBLE);
        }
    }
}
