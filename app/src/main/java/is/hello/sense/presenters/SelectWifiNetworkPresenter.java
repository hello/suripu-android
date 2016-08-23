package is.hello.sense.presenters;

import is.hello.sense.util.Analytics;

public abstract class SelectWifiNetworkPresenter extends ScopedPresenter<SelectWifiNetworkPresenter.Output>{

    @Override
    public void onDestroy() {

    }

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnScanAnalyticsEvent();

    public abstract String getOnRescanAnalyticsEvent();

    public interface Output extends is.hello.sense.presenters.outputs.Output {

    }


    public static class Onboarding extends SelectWifiNetworkPresenter {

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

    public static class Settings extends SelectWifiNetworkPresenter {

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
}
