package is.hello.sense.presenters.selectwifinetwork;

import android.support.annotation.NonNull;

import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class UpdateSelectWifiNetworkPresenter extends BaseSelectWifiNetworkPresenter {
    public UpdateSelectWifiNetworkPresenter(@NonNull final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_WIFI;
    }

    @Override
    public String getOnScanAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_WIFI_SCAN;
    }

    @Override
    public String getOnRescanAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_WIFI_RESCAN;
    }
}
