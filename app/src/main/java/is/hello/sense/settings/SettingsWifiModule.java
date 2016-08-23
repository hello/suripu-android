package is.hello.sense.settings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.presenters.ConnectWifiPresenter;
import is.hello.sense.presenters.SelectWifiNetworkPresenter;
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;

@Module(
        complete = false,
        injects = {
                SelectWiFiNetworkFragment.class,
                ConnectToWiFiFragment.class
        }
)
public class SettingsWifiModule {

    @Provides
    @Singleton
    SelectWifiNetworkPresenter providesSettingsSelectWifiNetworkPresenter(){
        return new SelectWifiNetworkPresenter.Settings();
    }

    @Provides
    @Singleton
    ConnectWifiPresenter providesSettingsConnectWifiPresenter(){
        return new ConnectWifiPresenter.Settings();
    }
}
