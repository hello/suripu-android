package is.hello.sense.presenters.selectwifinetwork;

import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class SettingsSelectWifiNetworkPresenter extends BaseSelectWifiNetworkPresenter {
    public SettingsSelectWifiNetworkPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_IN_APP;
    }

    @Override
    public String getOnScanAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_SCAN_IN_APP;
    }

    @Override
    public String getOnRescanAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_RESCAN_IN_APP;
    }
}
