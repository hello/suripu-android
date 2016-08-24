package is.hello.sense.presenters;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.outputs.BaseHardwareOutput;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public abstract class BasePairSensePresenter extends BaseHardwarePresenter<BasePairSensePresenter.Output> {

    public static final int REQUEST_CODE_EDIT_WIFI = 0xf1;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_SHOW_RATIONALE_DIALOG = 0xb2;

    private static final int RESULT_EDIT_WIFI = 0x99;

    private static final int LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI = 3;

    protected static final String OPERATION_LINK_ACCOUNT = "Linking account";

    private int linkAccountFailures = 0;

    protected static final String ARG_HAS_LINKED_ACCOUNT = "hasLinkedAccount";
    private boolean linkedAccount = false;

    private ApiService apiService;

    public BasePairSensePresenter(final HardwareInteractor hardwareInteractor,
                                  final UserFeaturesInteractor userFeaturesInteractor,
                                  final ApiService apiService){
        super(hardwareInteractor, userFeaturesInteractor);
        this.apiService = apiService;
    }

    @Override
    public void onDestroy() {
        //apiService = null;
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle state = new Bundle();
        state.putBoolean(ARG_HAS_LINKED_ACCOUNT, linkedAccount);
        return state;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        linkedAccount = savedState.getBoolean(ARG_HAS_LINKED_ACCOUNT);
    }

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubtitleRes();

    @StringRes
    public abstract int getPairingRes();

    @StringRes
    public abstract int getFinishedRes();

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnFinishAnalyticsEvent();

    protected abstract boolean shouldFinishFlow();

    protected abstract boolean shouldClearPeripheral();

    public boolean shouldShowPairDialog() {
        return false;
    }

    public void onPairSuccess(){
        if(shouldClearPeripheral()){
            hardwareInteractor.clearPeripheral();
        }
        if(shouldFinishFlow()){
            view.finishPairFlow(Activity.RESULT_OK);
        } else {
            view.finishActivity();
        }
    }

    public void checkLinkedAccount() {
        if(linkedAccount){
            finishUpOperations();
        } else {
            showBlockingActivity(R.string.title_linking_account);
            requestLinkAccount();
        }
    }

    public void updateLinkedAccount() {
        this.linkedAccount = true;
        finishUpOperations();
    }

    public boolean hasPeripheralPair() {
        Analytics.setSenseId(hardwareInteractor.getDeviceId());
        if (hardwareInteractor.getBondStatus() == GattPeripheral.BOND_BONDED) {
            showBlockingActivity(R.string.title_clearing_bond);
            return true;
        } else {
            showBlockingActivity(getPairingRes());
            return false;
        }
    }

    public boolean hasConnectivity(final ConnectProgress status) {
            if (status == ConnectProgress.CONNECTED) {
                showBlockingActivity(R.string.title_checking_connectivity);
                return true;
            } else {
                showBlockingActivity(Styles.getConnectStatusMessage(status));
                return false;
            }
    }

    public void requestLinkAccount() {
        bindAndSubscribe(hardwareInteractor.linkAccount(),
                         ignored -> updateLinkedAccount(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not link Sense to account", error);
                             presentError(error, OPERATION_LINK_ACCOUNT);
                         });
    }

    public void onLocationPermissionGranted() {
        showBlockingActivity(R.string.title_scanning_for_sense);
        final Observable<SensePeripheral> device = hardwareInteractor.closestPeripheral();
        bindAndSubscribe(device, this::tryToPairWith, e -> {
            hardwareInteractor.clearPeripheral();
            presentError(e, "Discovering Sense");
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            hardwareInteractor.setWantsHighPowerPreScan(true);
            view.next();
        } else if (requestCode == REQUEST_CODE_EDIT_WIFI && resultCode == RESULT_EDIT_WIFI) {
            view.finishPairFlow(REQUEST_CODE_EDIT_WIFI);
        } else if (requestCode == REQUEST_CODE_SHOW_RATIONALE_DIALOG && resultCode == Activity.RESULT_OK) {
            view.requestPermissionWithDialog();
        }
    }

    public void finishUpOperations() {
        setDeviceTimeZone();
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        final SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(getClass().getSimpleName(), "Time zone updated.");

                             pushDeviceData();
                         },
                         e -> presentError(e, "Updating time zone"));
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(hardwareInteractor.pushData(),
                         ignored -> getDeviceFeatures(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push Sense data, ignoring.", error);
                             getDeviceFeatures();
                         });
    }

    private void getDeviceFeatures() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(userFeaturesInteractor.storeFeaturesInPrefs(),
                         ignored -> view.onFinished(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not get features from Sense, ignoring.", error);
                             view.onFinished();
                         });
    }

    private void presentError(final Throwable e, final String operation) {
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
                                                            view.finishFactoryReset();
                                                            getOnboardingActivity().showSetupSense(); //todo return a flow result. Requires activity changes
                                                        }),
                                                        this::presentFactoryResetError), this::presentFactoryResetError);
        }
    }

    private void presentFactoryResetError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withOperation("Recovery Factory Reset")
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public interface Output extends BaseHardwareOutput {

        void onFinished();

        void finishPairFlow(int resultCode);

        void finishActivity();

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
    }
}
