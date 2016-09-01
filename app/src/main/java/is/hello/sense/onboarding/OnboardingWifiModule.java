package is.hello.sense.onboarding;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.OnboardingConnectWifiPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.OnboardingSelectWifiNetworkPresenter;
import is.hello.sense.ui.fragments.updating.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;

@Module(
        complete = false,
        injects = {
                SelectWifiNetworkFragment.class,
                ConnectToWiFiFragment.class
        }
)
public class OnboardingWifiModule {
    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                             @NonNull final ApiService apiService) {
        return new OnboardingConnectWifiPresenter(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new OnboardingSelectWifiNetworkPresenter(interactor);
    }
}
