package is.hello.sense.settings;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
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
    private final boolean shouldUseToolBar;

    public static SettingsPairSenseModule newPairOnlyInstance() {
        return new SettingsPairSenseModule(true, true);
    }

    public static SettingsPairSenseModule newEditWifiOnlyInstance() {
        return new SettingsPairSenseModule(false, false);
    }

    public SettingsPairSenseModule(final boolean shouldLinkAccount,
                                   final boolean shouldUseToolBar) {
        this.shouldLinkAccount = shouldLinkAccount;
        this.shouldUseToolBar = shouldUseToolBar;
    }

    @Provides
    @Singleton
    SettingsPairSenseInteractor providesSettingsPairSenseInteractor(final HardwareInteractor hardwareInteractor) {
        return new SettingsPairSenseInteractor(hardwareInteractor);
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final DevicesInteractor devicesInteractor,
                                                             @NonNull final ApiService apiService,
                                                             @NonNull final SettingsPairSenseInteractor pairSenseInteractor,
                                                             @NonNull final PreferencesInteractor preferencesInteractor) {
        return new SettingsConnectWifiPresenter(hardwareInteractor,
                                                devicesInteractor,
                                                apiService,
                                                pairSenseInteractor,
                                                preferencesInteractor,
                                                shouldLinkAccount,
                                                shouldUseToolBar);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new SettingsSelectWifiNetworkPresenter(interactor,
                                                      shouldUseToolBar);
    }

    @Provides
    @Singleton
    PairSensePresenter providesSettingsPairSensePresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                          @NonNull final DevicesInteractor devicesInteractor,
                                                          @NonNull final ApiService apiService,
                                                          @NonNull final SettingsPairSenseInteractor pairSenseInteractor,
                                                          @NonNull final PreferencesInteractor preferencesInteractor) {
        return new SettingsPairSensePresenter(hardwareInteractor,
                                              devicesInteractor,
                                              apiService,
                                              pairSenseInteractor,
                                              preferencesInteractor);
    }
}
