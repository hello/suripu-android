package is.hello.sense.onboarding;

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
public class OnboardingWifiModule {
    @Provides
    @Singleton
    SelectWifiNetworkPresenter providesOnboardingSelectWifiNetworkPresenter(){
        return new SelectWifiNetworkPresenter.Onboarding();
    }

    @Provides
    @Singleton
    ConnectWifiPresenter providesOnboardingConnectWifiPresenter(){
        return new ConnectWifiPresenter.Onboarding();
    }
}
