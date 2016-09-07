package is.hello.sense.presenters.connectwifi;

import android.support.annotation.NonNull;

import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.util.Analytics;

public class UpgradeConnectWifiPresenter extends BaseConnectWifiPresenter {


    public UpgradeConnectWifiPresenter(
            @NonNull final HardwareInteractor hardwareInteractor,
            @NonNull final UserFeaturesInteractor userFeaturesInteractor,
            @NonNull final ApiService apiService,
            @NonNull final PairSenseInteractor pairSenseInteractor) {
        super(hardwareInteractor, userFeaturesInteractor, apiService, pairSenseInteractor);
    }

    @Override
    protected boolean shouldLinkAccount() {
        return true;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_WIFI_PASSWORD;
    }

    @Override
    public String getOnSubmitWifiCredentialsAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_WIFI_CREDENTIALS_SUBMITTED;
    }

    @Override
    public String getWifiAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_SENSE_WIFI_UPDATE;
    }
}
