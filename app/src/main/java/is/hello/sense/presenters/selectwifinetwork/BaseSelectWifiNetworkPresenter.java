package is.hello.sense.presenters.selectwifinetwork;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.Collection;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.presenters.BaseHardwarePresenter;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public abstract class BaseSelectWifiNetworkPresenter
        extends BaseHardwarePresenter<BaseSelectWifiNetworkPresenter.Output> {

    public BaseSelectWifiNetworkPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private final static String OPERATION_SCAN_FOR_NETWORK = "Scan for networks";

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnScanAnalyticsEvent();

    public abstract String getOnRescanAnalyticsEvent();

    public void sendOnScanAnalytics() {
        Analytics.trackEvent(getOnScanAnalyticsEvent(), null);
    }

    private void sendOnRescanAnalytics() {
        Analytics.trackEvent(getOnRescanAnalyticsEvent(), null);
    }

    public void initialScan() {
        sendOnScanAnalytics();
        rescan(false);
    }

    @SuppressWarnings("unused")
    public void onRescanButtonClicked(@Nullable final View clickedView){
        sendOnRescanAnalytics();
        rescan(true);
    }

    protected void rescan(final boolean sendCountryCode) {
        execute( () -> view.showScanning());
        if (!hardwareInteractor.hasPeripheral()) {
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(),
                             ignored -> rescan(sendCountryCode),
                             this::onPeripheralDiscoveryError);
            return;
        }

        if (!hardwareInteractor.isConnected()) {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(),
                             status -> {
                                 if (status != ConnectProgress.CONNECTED) {
                                     return;
                                 }
                                 rescan(sendCountryCode);
                             }, this::onPeripheralDiscoveryError);

            return;
        }

        showHardwareActivity(() -> {
            logEvent("inside onComplete showHardwareActivity");
            bindAndSubscribe(hardwareInteractor.scanForWifiNetworks(sendCountryCode),
                             this::bindScanResults,
                             this::onWifiError);
        }, this::onWifiError);
    }

    private void bindScanResults(@NonNull final Collection<SenseCommandProtos.wifi_endpoint> scanResults) {
        hideHardwareActivity(() -> {
            view.bindScanResults(scanResults);
            view.showRescanOption();
        }, null);
    }

    private void onPeripheralDiscoveryError(final Throwable e) {
        execute(() -> {
            view.showRescanOption();
            presentErrorDialog(e, OPERATION_SCAN_FOR_NETWORK);
        });
    }

    private void onWifiError(final Throwable e) {
        hideHardwareActivity(() -> {
            view.showRescanOption();
            presentErrorDialog(e, OPERATION_SCAN_FOR_NETWORK);
        }, null);
    }

    private void presentErrorDialog(@NonNull final Throwable e, final String operation){
        final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
        builder.withOperation(operation);
        view.showErrorDialog(builder);
    }

    public interface Output extends BaseOutput {

        void showScanning();

        void showRescanOption();

        void bindScanResults(Collection<SenseCommandProtos.wifi_endpoint> scanResults);
    }

}
