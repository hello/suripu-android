package is.hello.sense.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.concurrent.TimeUnit;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public abstract class BasePairSensePresenter<T extends BasePairSensePresenter.Output> extends BaseHardwarePresenter<T> {

    protected static final String OPERATION_LINK_ACCOUNT = "Linking account";
    protected static final String ARG_HAS_LINKED_ACCOUNT = "hasLinkedAccount";

    private boolean linkedAccount = false;

    private final ApiService apiService;
    protected final DevicesInteractor devicesInteractor;
    private final PairSenseInteractor pairSenseInteractor;
    protected final PreferencesInteractor preferencesInteractor;

    @NonNull
    private Subscription devicesSubscription = Subscriptions.empty();

    public BasePairSensePresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                  @NonNull final DevicesInteractor devicesInteractor,
                                  @NonNull final ApiService apiService,
                                  @NonNull final PairSenseInteractor pairSenseInteractor,
                                  @NonNull final PreferencesInteractor preferencesInteractor) {
        super(hardwareInteractor);
        this.devicesInteractor = devicesInteractor;
        this.apiService = apiService;
        this.pairSenseInteractor = pairSenseInteractor;
        this.preferencesInteractor = preferencesInteractor;
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
        super.onRestoreState(savedState);
        linkedAccount = savedState.getBoolean(ARG_HAS_LINKED_ACCOUNT);
    }

    public abstract String getOnCreateAnalyticsEvent();

    protected abstract void presentError(Throwable e, String operation);

    @StringRes
    public int getPairingRes() {
        return pairSenseInteractor.getPairingRes();
    }

    @StringRes
    public int getFinishedRes() {
        return pairSenseInteractor.getFinishedRes();
    }

    protected boolean shouldContinueFlow() {
        return pairSenseInteractor.shouldContinueFlow();
    }

    protected boolean shouldResetOnPairSuccess() {
        return pairSenseInteractor.shouldClearPeripheral();
    }

    protected Observable<SensePeripheral> getObservableSensePeripheral() {
        return pairSenseInteractor.closestPeripheral();
    }

    protected String getOnFinishAnalyticsEvent() {
        return pairSenseInteractor.getOnFinishedAnalyticsEvent();
    }

    protected void sendOnFinishedAnalytics() {
        Analytics.trackEvent(getOnFinishAnalyticsEvent(), null);
    }

    @StringRes
    public int getLinkedAccountErrorTitleRes() {
        return pairSenseInteractor.getLinkedAccountErrorTitleRes();
    }

    protected void onPairSuccess() {
        if (shouldResetOnPairSuccess()) {
            hardwareInteractor.reset();
        }
        if (shouldContinueFlow()) {
            view.finishFlowWithResult(Activity.RESULT_OK);
        } else {
            view.finishActivity();
        }
    }

    public void checkLinkedAccount() {
        if (linkedAccount) {
            finishUpOperations();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            requestLinkAccount();
        }
    }

    protected void updateLinkedAccount() {
        this.linkedAccount = true;
        finishUpOperations();
    }

    protected void requestLinkAccount() {
        bindAndSubscribe(pairSenseInteractor.linkAccount(),
                         ignored -> updateLinkedAccount(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not link Sense to account", error);
                             presentError(error, OPERATION_LINK_ACCOUNT);
                         });
    }

    protected boolean hasPeripheralPair() {
        Analytics.setSenseId(hardwareInteractor.getDeviceId());
        if (hardwareInteractor.isBonded()) {
            showBlockingActivity(R.string.title_clearing_bond);
            return true;
        } else {
            showBlockingActivity(getPairingRes());
            return false;
        }
    }

    protected boolean hasConnectivity(final ConnectProgress status) {
        if (status == ConnectProgress.CONNECTED) {
            showBlockingActivity(R.string.title_checking_connectivity);
            return true;
        } else {
            showBlockingActivity(Styles.getConnectStatusMessage(status));
            return false;
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
                         ignored -> getDeviceFeatures(false),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push Sense data, ignoring.", error);
                             getDeviceFeatures(false);
                         });
    }

    /**
     * @param isRetrying should be true if using from any thread not the UI thread. Trying to show
     *                   the blocking activity from another thread will trigger an error and not
     *                   show voice tutorial if the user has it.
     *                   <p>
     *                   todo clean up how this presenter is showing blocking activities (will be done as we transition to viper2).
     */
    private void getDeviceFeatures(final boolean isRetrying) {
        if (!isRetrying) {
            showBlockingActivity(R.string.title_pushing_data);
        }
        //todo figure out a way to automatically unsubscribe after first value is received.
        //todo should also consider forgetting the subject here.
        devicesSubscription.unsubscribe();
        devicesSubscription = bind(devicesInteractor.devices)
                .delay(2, TimeUnit.SECONDS)
                .subscribe(devices -> {
                               devicesSubscription.unsubscribe(); // Unsubscribe so onfinished isn't called multiple times. Triggering an error for too many BLE commands at once
                               if (devices == null || devices.getSense() == null) {
                                   getDeviceFeatures(true); // servers not in sync. try again.
                               } else {
                                   preferencesInteractor.setDevice(devices.getSense());
                                   onFinished();
                               }
                           },
                           error -> {
                               devicesSubscription.unsubscribe();// Unsubscribe so onfinished isn't called multiple times. Triggering an error for too many BLE commands at once
                               Logger.error(getClass().getSimpleName(), "Could not get features from Sense, ignoring.", error);
                               onFinished();
                           });
        devicesInteractor.update();

    }

    private void onFinished() {
        hideAllActivityForSuccess(getFinishedRes(),
                                  () -> {
                                      sendOnFinishedAnalytics();
                                      onPairSuccess();
                                  },
                                  e -> {
                                      presentError(e, "Turning off LEDs");
                                  });
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseSubscription();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        releaseSubscription();
    }

    private void releaseSubscription() {
        devicesSubscription.unsubscribe();
        devicesSubscription = Subscriptions.empty();
    }

    public interface Output extends BaseOutput {


    }
}
