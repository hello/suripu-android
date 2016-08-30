package is.hello.sense.presenters.selectwifinetwork;

import android.support.annotation.NonNull;

import java.util.Collection;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.presenters.BaseHardwarePresenter;
import is.hello.sense.presenters.outputs.BaseOutput;

public abstract class BaseSelectWifiNetworkPresenter
        extends BaseHardwarePresenter<BaseSelectWifiNetworkPresenter.Output> {

    public BaseSelectWifiNetworkPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    public void onDetach() {

    }

    private final static String SCAN_FOR_NETWORK_OPERATION = "Scan for networks";

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnScanAnalyticsEvent();

    public abstract String getOnRescanAnalyticsEvent();

    public void rescan(final boolean sendCountryCode) {
        view.showScanning();
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
            view.presentErrorDialog(e, SCAN_FOR_NETWORK_OPERATION);
        });
    }

    private void onWifiError(final Throwable e) {
        hideHardwareActivity(() -> {
            view.showRescanOption();
            view.presentErrorDialog(e, SCAN_FOR_NETWORK_OPERATION);
        }, null);
    }

    public interface Output extends BaseOutput {

        void showScanning();

        void showRescanOption();

        void bindScanResults(Collection<SenseCommandProtos.wifi_endpoint> scanResults);

        void presentErrorDialog(Throwable e, String operation);
    }

}
