package is.hello.sense.settings;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.SettingsConnectWifiPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.SettingsSelectWifiNetworkPresenter;
import is.hello.sense.ui.fragments.updating.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;

@Module(
        complete = false,
        injects = {
                SelectWifiNetworkFragment.class,
                ConnectToWiFiFragment.class
        }
)
public class SettingsWifiModule {
    private final boolean shouldLinkAccount;

    public SettingsWifiModule(final boolean shouldLinkAccount) {
        this.shouldLinkAccount = shouldLinkAccount;
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                             @NonNull final ApiService apiService) {
        return new SettingsConnectWifiPresenter(hardwareInteractor, userFeaturesInteractor, apiService, shouldLinkAccount);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new SettingsSelectWifiNetworkPresenter(interactor);
    }
}
