package is.hello.sense.presenters.connectwifi;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.util.Analytics;

public class SettingsConnectWifiPresenter extends BaseConnectWifiPresenter {

    private final boolean shouldLinkAccount;

    public SettingsConnectWifiPresenter(
            @NonNull final HardwareInteractor hardwareInteractor,
            @NonNull final DevicesInteractor devicesInteractor,
            @NonNull final ApiService apiService,
            @NonNull final PairSenseInteractor pairSenseInteractor,
            @NonNull final PreferencesInteractor preferencesInteractor,
            final boolean shouldLinkAccount) {
        super(hardwareInteractor, devicesInteractor, apiService, pairSenseInteractor, preferencesInteractor);
        this.shouldLinkAccount = shouldLinkAccount;
    }

    @Override
    protected boolean shouldUseToolbar() {
        return false;
    }

    @Override
    protected boolean shouldLinkAccount() {
        return shouldLinkAccount;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Settings.EVENT_WIFI_PASSWORD;
    }

    @Override
    public String getOnSubmitWifiCredentialsAnalyticsEvent() {
        return Analytics.Settings.EVENT_WIFI_CREDENTIALS_SUBMITTED;
    }

    @Override
    public String getWifiAnalyticsEvent() {
        return Analytics.Settings.EVENT_SENSE_WIFI_UPDATE;
    }

    @Override
    public int getLinkedAccountErrorTitleRes() {
        return R.string.error_title_wifi_connection;
    }
}
