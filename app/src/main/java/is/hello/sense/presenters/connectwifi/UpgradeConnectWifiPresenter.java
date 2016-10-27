package is.hello.sense.presenters.connectwifi;

import android.support.annotation.NonNull;

import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.util.Analytics;

public class UpgradeConnectWifiPresenter extends BaseConnectWifiPresenter {


    public UpgradeConnectWifiPresenter(
            @NonNull final HardwareInteractor hardwareInteractor,
            @NonNull final DevicesInteractor devicesInteractor,
            @NonNull final ApiService apiService,
            @NonNull final PairSenseInteractor pairSenseInteractor,
            @NonNull final PreferencesInteractor preferencesInteractor) {
        super(hardwareInteractor, devicesInteractor, apiService, pairSenseInteractor, preferencesInteractor);
    }

    @Override
    protected boolean shouldLinkAccount() {
        return true;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Upgrade.EVENT_WIFI_PASSWORD;
    }

    @Override
    public String getOnSubmitWifiCredentialsAnalyticsEvent() {
        return Analytics.Upgrade.EVENT_WIFI_CREDENTIALS_SUBMITTED;
    }

    @Override
    public String getWifiAnalyticsEvent() {
        return Analytics.Upgrade.EVENT_SENSE_WIFI_UPDATE;
    }
}
