package is.hello.sense.presenters;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.util.Analytics;

public abstract class ConnectWifiPresenter extends ScopedPresenter<ConnectWifiPresenter.Output>{

    @Override
    public void onDestroy() {

    }

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnSubmitWifiCredentialsAnalyticsEvent();

    public abstract String getWifiAnalyticsEvent();


    public interface Output extends BaseOutput{

    }

    public static class Onboarding extends ConnectWifiPresenter {

        @Override
        public String getOnCreateAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_WIFI_PASSWORD;
        }

        @Override
        public String getOnSubmitWifiCredentialsAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED_IN_APP;
        }

        @Override
        public String getWifiAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE;
        }
    }

    public static class Settings extends ConnectWifiPresenter {

        @Override
        public String getOnCreateAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_WIFI_PASSWORD_IN_APP;
        }

        @Override
        public String getOnSubmitWifiCredentialsAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED;
        }

        @Override
        public String getWifiAnalyticsEvent() {
            return Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE_IN_APP;
        }
    }
}
