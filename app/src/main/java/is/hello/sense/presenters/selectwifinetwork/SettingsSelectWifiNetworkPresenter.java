package is.hello.sense.presenters.selectwifinetwork;

import android.support.annotation.NonNull;

import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class SettingsSelectWifiNetworkPresenter extends BaseSelectWifiNetworkPresenter {
    public SettingsSelectWifiNetworkPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    protected boolean shouldUseToolBar() {
        return false;
    }

    @Override
    public boolean onBackPressed(@NonNull final Runnable ignored) {
        if (shouldUseDefaultBackPressedBehavior()) {
            execute(() -> view.cancelFlow());
        }
        return true;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Settings.EVENT_WIFI;
    }

    @Override
    public String getOnScanAnalyticsEvent() {
        return Analytics.Settings.EVENT_WIFI_SCAN;
    }

    @Override
    public String getOnRescanAnalyticsEvent() {
        return Analytics.Settings.EVENT_WIFI_RESCAN;
    }
}
