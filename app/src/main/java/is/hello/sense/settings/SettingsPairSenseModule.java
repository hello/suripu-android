package is.hello.sense.settings;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.SettingsPairSenseInteractor;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.presenters.SettingsPairSensePresenter;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.SettingsConnectWifiPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.SettingsSelectWifiNetworkPresenter;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.updating.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;

@Module(
        complete = false,
        injects = {
                SelectWifiNetworkFragment.class,
                ConnectToWiFiFragment.class,
                PairSenseFragment.class
        }
)
public class SettingsPairSenseModule {
    private final boolean shouldLinkAccount;

    public SettingsPairSenseModule(final boolean shouldLinkAccount) {
        this.shouldLinkAccount = shouldLinkAccount;
    }

    @Provides
    @Singleton
    SettingsPairSenseInteractor providesSettingsPairSenseInteractor(final HardwareInteractor hardwareInteractor){
        return new SettingsPairSenseInteractor(hardwareInteractor);
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                             @NonNull final ApiService apiService,
                                                             @NonNull final SettingsPairSenseInteractor pairSenseInteractor) {
        return new SettingsConnectWifiPresenter(hardwareInteractor,
                                                userFeaturesInteractor,
                                                apiService,
                                                pairSenseInteractor,
                                                shouldLinkAccount);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new SettingsSelectWifiNetworkPresenter(interactor);
    }

    @Provides
    @Singleton
    PairSensePresenter providesSettingsPairSensePresenter(final HardwareInteractor hardwareInteractor,
                                                          final UserFeaturesInteractor userFeaturesInteractor,
                                                          final ApiService apiService,
                                                          final SettingsPairSenseInteractor pairSenseInteractor) {
        return new SettingsPairSensePresenter(hardwareInteractor,
                                              userFeaturesInteractor,
                                              apiService,
                                              pairSenseInteractor);
    }
}
