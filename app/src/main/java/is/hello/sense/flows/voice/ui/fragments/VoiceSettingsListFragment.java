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
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.flows.voice.ui.views.VoiceSettingsListView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.mvp.fragments.SenseViewFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class VoiceSettingsListFragment extends SenseViewFragment<VoiceSettingsListView> {

    @Inject
    VoiceSettingsInteractor settingsInteractor;

    @Inject
    CurrentSenseInteractor currentSenseInteractor;

    public static final int RESULT_VOLUME_SELECTED = 99;
    private static final int RESULT_CANCEL_FLOW = 104;
    private Subscription updateSettingsSubscription = Subscriptions.empty();

    @Override
    public void initializeSenseView() {
        if (senseView == null) {
            senseView = new VoiceSettingsListView(getActivity());
            senseView.setVolumeClickListener(this::redirectToVolumeSelection);
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
                         this::presentDeviceUnavailable);

        bindAndSubscribe(settingsInteractor.settingsSubject,
                         this::bindSettings,
                         this::presentSettingsUnavailable);

        currentSenseInteractor.update();
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_CANCEL_FLOW){
            cancelFlow();
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
        senseView.showFirmwareUpdateCard(false);
        senseView.updateVolumeTextView(settings);

        senseView.updateMuteSwitch(settings.isMuteOrDefault(),
                                   this::onMuteSwitchChanged);

        if (settings.isPrimaryUserOrDefault()) {
            senseView.makePrimaryUser();
        } else {
            senseView.makeSecondaryUser(this::showPrimaryUserDialog);
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
        showLockedBlockingActivity(R.string.voice_settings_progress_updating);
        updateSettingsSubscription.unsubscribe();
        updateSettingsSubscription = bind(updateObservable)
                .subscribe(Functions.NO_OP,
                           this::presentUpdateError,
                           () -> hideBlockingActivity(true, null));

    }

    private void presentUpdateError(@NonNull final Throwable e) {
        showProgress(false);
        if (e instanceof VoiceSettingsInteractor.SettingsUpdateThrowable) {
            final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(e);
            builder.withTitle(StringRef.from(R.string.voice_settings_update_error_title));
            showErrorDialog(builder);
            if (e instanceof VoiceSettingsInteractor.MuteUpdateThrowable) {
                senseView.flipMuteSwitch(this::onMuteSwitchChanged);
            } else if (e instanceof VoiceSettingsInteractor.PrimaryUpdateThrowable) {
                senseView.makeSecondaryUser(this::showPrimaryUserDialog);
            }
        } else {
            showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
        }
    }

    private void presentDeviceUnavailable(@NonNull final Throwable e) {
        hideBlockingActivity(false, null);
        senseView.setVisibility(View.INVISIBLE);
        showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e),
                        RESULT_CANCEL_FLOW);
    }

    private void presentSettingsUnavailable(@NonNull final Throwable e) {
        if(ApiException.statusEquals(e, 400) || ApiException.statusEquals(e, 412)) {
            showProgress(false);
            senseView.showFirmwareUpdateCard(true);
        } else {
            presentDeviceUnavailable(e);
        }
    }

    private void redirectToVolumeSelection(final View ignore) {
        finishFlowWithResult(RESULT_VOLUME_SELECTED);
    }

    private void showProgress(final boolean show) {
        if (show) {
            senseView.setVisibility(View.INVISIBLE);
            showBlockingActivity(null);
        } else {
            hideBlockingActivity(false, null);
            senseView.setVisibility(View.VISIBLE);
        }
    }
}
