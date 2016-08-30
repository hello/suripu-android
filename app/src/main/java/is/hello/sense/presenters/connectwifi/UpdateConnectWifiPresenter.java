package is.hello.sense.presenters.connectwifi;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.util.Analytics;

public class UpdateConnectWifiPresenter extends BaseConnectWifiPresenter {


    public UpdateConnectWifiPresenter(
            @NonNull final HardwareInteractor hardwareInteractor,
            @NonNull final UserFeaturesInteractor userFeaturesInteractor,
            @NonNull final ApiService apiService) {
        super(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @Override
    public int getPairingRes() {
        return R.string.title_pairing_with_sense;
    }

    @Override
    public int getFinishedRes() {
        return R.string.title_paired;
    }


    @Override
    public String getOnFinishAnalyticsEvent() {
        return getOnSubmitWifiCredentialsAnalyticsEvent();
    }

    @Override
    protected boolean shouldContinueFlow() {
        return true;
    }

    @Override
    protected boolean shouldClearPeripheral() {
        return true;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_PASSWORD;
    }

    @Override
    public String getOnSubmitWifiCredentialsAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED;
    }

    @Override
    public String getWifiAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE;
    }
}
