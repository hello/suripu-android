package is.hello.sense.presenters.selectwifinetwork;

import android.support.annotation.NonNull;

import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class OnboardingSelectWifiNetworkPresenter extends BaseSelectWifiNetworkPresenter {
    public OnboardingSelectWifiNetworkPresenter(@NonNull final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI;
    }

    @Override
    public String getOnScanAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_SCAN;
    }

    @Override
    public String getOnRescanAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_RESCAN;
    }
}
