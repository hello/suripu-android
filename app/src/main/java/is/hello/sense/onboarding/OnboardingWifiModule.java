package is.hello.sense.onboarding;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.interactors.HardwareInteractor;
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
    SelectWifiNetworkPresenter providesOnboardingSelectWifiNetworkPresenter(
            final HardwareInteractor hardwareInteractor){
        return new SelectWifiNetworkPresenter.Onboarding( hardwareInteractor);
    }

    @Provides
    @Singleton
    ConnectWifiPresenter providesOnboardingConnectWifiPresenter(
            final HardwareInteractor hardwareInteractor){
        return new ConnectWifiPresenter.Onboarding( hardwareInteractor);
    }
}
