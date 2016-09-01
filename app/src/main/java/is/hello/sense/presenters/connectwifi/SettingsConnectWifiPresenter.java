package is.hello.sense.presenters.connectwifi;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.util.Analytics;

public class SettingsConnectWifiPresenter extends BaseConnectWifiPresenter {


    public SettingsConnectWifiPresenter(
            @NonNull final HardwareInteractor hardwareInteractor,
            @NonNull final UserFeaturesInteractor userFeaturesInteractor,
            @NonNull final ApiService apiService) {
        super(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @Override
    protected boolean shouldSendAccessToken() {
        return false;
    }

    @Override
    public int getPairingRes() {
        return R.string.title_connecting_with_sense;
    }

    @Override
    public int getFinishedRes() {
        return R.string.action_done;
    }

    @Override
    public String getOnFinishAnalyticsEvent() {
        return getOnSubmitWifiCredentialsAnalyticsEvent();
    }

    @Override
    protected boolean shouldContinueFlow() {
        return false;
    }

    @Override
    protected boolean shouldClearPeripheral() {
        return false;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_PASSWORD_IN_APP;
    }

    @Override
    public String getOnSubmitWifiCredentialsAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED_IN_APP;
    }

    @Override
    public String getWifiAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE_IN_APP;
    }

    @Override
    public int getLinkedAccountErrorTitleRes() {
        return R.string.error_title_wifi_connection;
    }
}
