package is.hello.sense.onboarding;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
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
            final HardwareInteractor hardwareInteractor,
            final UserFeaturesInteractor userFeaturesInteractor){
        return new SelectWifiNetworkPresenter.Onboarding( hardwareInteractor, userFeaturesInteractor);
    }

    @Provides
    @Singleton
    ConnectWifiPresenter providesOnboardingConnectWifiPresenter(
            final HardwareInteractor hardwareInteractor,
            final UserFeaturesInteractor userFeaturesInteractor){
        return new ConnectWifiPresenter.Onboarding( hardwareInteractor, userFeaturesInteractor);
    }
}
