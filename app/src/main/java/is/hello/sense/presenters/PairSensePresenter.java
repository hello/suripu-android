package is.hello.sense.presenters;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
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
                              final ApiService apiService) {
        super(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubtitleRes();

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
        view.finishPairFlow(REQUEST_CODE_EDIT_WIFI);
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
        final Observable<SensePeripheral> device = hardwareInteractor.closestPeripheral();
        bindAndSubscribe(device, this::tryToPairWith, e -> {
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
                             ignored -> hasPeripheralPair()
                    ,
                             e -> presentError(e, "Clearing Bond"));
        } else {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(),
                             status -> {
                                 if(hasConnectivity(status)){
                                     checkConnectivityAndContinue();
                                 }},
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
                    view.presentError(StringRef.from(R.string.error_link_account_failed_multiple_times),
                                      RESULT_EDIT_WIFI, R.string.action_select_wifi_network,
                                      operation,
                                      REQUEST_CODE_EDIT_WIFI);

                    Analytics.trackError(e, operation);
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
                view.presentUnstableBluetoothDialog(e, operation);
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
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(),
                             state -> {
                                 if (state != ConnectProgress.CONNECTED) {
                                     return;
                                 }
                                 performRecoveryFactoryReset();
                             },
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
        hideBlockingActivity(false, () -> view.presentFactoryResetDialog(e, "Recovery Factory Reset"));
    }



    public interface Output extends BasePairSensePresenter.Output{
        void requestPermissionWithDialog();

        void presentError(StringRef message,
                          int resultCode,
                          @StringRes int actionStringRes,
                          String operation,
                          int requestCode);

        void presentHighPowerErrorDialog(int requestCode);

        void presentTroubleShootingDialog();

        void presentUnstableBluetoothDialog(Throwable e, String operation);

        void showMessageDialog(@StringRes int titleRes, @StringRes int messageRes);

        void presentFactoryResetDialog(Throwable e, String operation);

        void showPairDialog(String deviceName,
                            Action0 positiveAction,
                            Action0 negativeAction);

        void onPrimaryButtonClicked();
    }
}
