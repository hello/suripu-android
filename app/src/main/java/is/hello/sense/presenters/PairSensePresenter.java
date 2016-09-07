package is.hello.sense.presenters;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.functions.Action0;

public abstract class PairSensePresenter extends BasePairSensePresenter<PairSensePresenter.Output> {

    public static final int REQUEST_CODE_EDIT_WIFI = 0xf1;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_SHOW_RATIONALE_DIALOG = 0xb2;

    private static final int RESULT_EDIT_WIFI = 0x99;
    private static final int LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI = 3;
    private int linkAccountFailures = 0;


    public PairSensePresenter(final HardwareInteractor hardwareInteractor,
                              final UserFeaturesInteractor userFeaturesInteractor,
                              final ApiService apiService,
                              final PairSenseInteractor pairSenseInteractor) {
        super(hardwareInteractor, userFeaturesInteractor, apiService, pairSenseInteractor);
    }

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubtitleRes();

    public abstract String getAnalyticsHelpEvent();

    public abstract boolean showSupportOptions();

    public boolean shouldShowPairDialog() {
        return false;
    }

    private void continueToWifi() {
        hideAllActivityForSuccess(getFinishedRes(),
                                  this::showSelectWifiNetwork,
                                  e -> presentError(e, "Turning off LEDs"));
    }

    private void showSelectWifiNetwork() {
        view.finishFlowWithResult(REQUEST_CODE_EDIT_WIFI);
    }

    @SuppressWarnings("unused")
    public void showPairingModeHelp(@NonNull final View ignore) {
        Analytics.trackEvent(getAnalyticsHelpEvent(), null);
        view.showHelpUri(UserSupport.HelpStep.PAIRING_MODE);
    }

    public void showToolbarHelp() {
        view.showHelpUri(UserSupport.HelpStep.PAIRING_SENSE_BLE);
    }

    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            hardwareInteractor.setWantsHighPowerPreScan(true);
            view.onPrimaryButtonClicked();
        } else if (requestCode == REQUEST_CODE_EDIT_WIFI && resultCode == RESULT_EDIT_WIFI) {
            showSelectWifiNetwork();
        } else if (requestCode == REQUEST_CODE_SHOW_RATIONALE_DIALOG && resultCode == Activity.RESULT_OK) {
            view.requestPermissionWithDialog();
        }
    }

    public void onLocationPermissionGranted() {
        showBlockingActivity(R.string.title_scanning_for_sense);
        final Observable<SensePeripheral> device = getObservableSensePeripheral();
        bindAndSubscribe(device,
                         this::tryToPairWith,
                         e -> {
                             hardwareInteractor.clearPeripheral();
                             presentError(e, "Discovering Sense");
                         });
    }

    private void tryToPairWith(@NonNull final SensePeripheral device) {
        if (shouldShowPairDialog()) {
            view.showPairDialog(
                    device.getName(),
                    this::completePeripheralPair,
                    () -> hideBlockingActivity(false, hardwareInteractor::clearPeripheral));
        } else {
            completePeripheralPair();
        }
    }

    public void completePeripheralPair() {
        if (hasPeripheralPair()) {
            bindAndSubscribe(hardwareInteractor.clearBond(),
                             ignored -> completePeripheralPair()
                    ,
                             e -> presentError(e, "Clearing Bond"));
        } else {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(),
                             status -> {
                                 if (hasConnectivity(status)) {
                                     checkConnectivityAndContinue();
                                 }
                             },
                             e -> presentError(e, "Connecting to Sense"));
        }
    }

    private void checkConnectivityAndContinue() {
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwareInteractor.currentWifiNetwork(), network -> {
                if (network.connectionState == SenseCommandProtos.wifi_connection_state.IP_RETRIEVED) {
                    checkLinkedAccount();
                } else {
                    continueToWifi();
                }
            }, e -> {
                Logger.error(getClass().getSimpleName(), "Could not get Sense's wifi network", e);
                continueToWifi();
            });
        }, e -> presentError(e, "Turning on LEDs"));
    }

    protected void presentError(final Throwable e, final String operation) {
        hideAllActivityForFailure(() -> {
            if (OPERATION_LINK_ACCOUNT.equals(operation)) {
                this.linkAccountFailures++;
                if (linkAccountFailures >= LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI) {
                    final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
                    builder.withOperation(operation)
                           .withMessage(StringRef.from(R.string.error_link_account_failed_multiple_times))
                            .withAction(RESULT_EDIT_WIFI,  R.string.action_select_wifi_network)
                            .withSupportLink()
                            .build();
                    view.showErrorDialog(builder, REQUEST_CODE_EDIT_WIFI);
                    return;
                }
            }

            if (e instanceof SenseNotFoundError) {
                hardwareInteractor.trackPeripheralNotFound();

                if (hardwareInteractor.shouldPromptForHighPowerScan()) {
                    view.presentHighPowerErrorDialog(REQUEST_CODE_HIGH_POWER_RETRY);
                } else {
                    view.presentTroubleShootingDialog();
                }

                Analytics.trackError(e, operation);
            } else {
                final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
                builder.withTitle(getLinkedAccountErrorTitleRes())
                       .withOperation(operation);
                view.showErrorDialog(builder);
            }
        });
    }

    public void performRecoveryFactoryReset() {
        showBlockingActivity(R.string.dialog_loading_message);

        if (!hardwareInteractor.hasPeripheral()) {
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(),
                             ignored -> performRecoveryFactoryReset(),
                             this::presentFactoryResetError);
        } else if (!hardwareInteractor.isConnected()) {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral()
                                               .filter(ConnectProgress.CONNECTED::equals),
                             ignored -> performRecoveryFactoryReset(),
                             this::presentFactoryResetError);
        } else {
            showHardwareActivity(() -> bindAndSubscribe(hardwareInteractor.unsafeFactoryReset(),
                                                        ignored -> hideBlockingActivity(true, () -> {
                                                            Analytics.setSenseId("unpaired");
                                                            userFeaturesInteractor.reset();
                                                            view.showMessageDialog(R.string.title_power_cycle_sense_factory_reset,
                                                                                   R.string.message_power_cycle_sense_factory_reset);


                                                        }),
                                                        this::presentFactoryResetError),
                                 this::presentFactoryResetError);
        }
    }

    private void presentFactoryResetError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
            builder.withOperation("Recovery Factory Reset")
                    .withSupportLink();
            view.showErrorDialog(builder);
        });
    }

    public interface Output extends BasePairSensePresenter.Output {
        void requestPermissionWithDialog();

        void showErrorDialog(ErrorDialogFragment.PresenterBuilder builder, int targetFragmentRequestCode);

        void presentHighPowerErrorDialog(int requestCode);

        void presentTroubleShootingDialog();

        void showMessageDialog(@StringRes int titleRes, @StringRes int messageRes);

        void showPairDialog(String deviceName,
                            Action0 positiveAction,
                            Action0 negativeAction);

        void onPrimaryButtonClicked();
    }
}
