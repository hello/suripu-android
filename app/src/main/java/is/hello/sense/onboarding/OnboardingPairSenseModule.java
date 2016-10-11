package is.hello.sense.onboarding;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.OnboardingPairSenseInteractor;
import is.hello.sense.presenters.OnboardingPairSensePresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.OnboardingConnectWifiPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.OnboardingSelectWifiNetworkPresenter;
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
public class OnboardingPairSenseModule {

    @Provides
    @Singleton
    OnboardingPairSenseInteractor providesOnboardingPairSenseInteractor(final HardwareInteractor hardwareInteractor) {
        return new OnboardingPairSenseInteractor(hardwareInteractor);
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                             @NonNull final ApiService apiService,
                                                             @NonNull final OnboardingPairSenseInteractor pairSenseInteractor,
                                                             @NonNull final PreferencesInteractor preferencesInteractor) {
        return new OnboardingConnectWifiPresenter(hardwareInteractor, userFeaturesInteractor, apiService, pairSenseInteractor,preferencesInteractor);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new OnboardingSelectWifiNetworkPresenter(interactor);
    }

    @Provides
    @Singleton
    PairSensePresenter providesOnboardingPairSensePresenter(@NonNull final HardwareInteractor interactor,
                                                            @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                            @NonNull final ApiService apiService,
                                                            @NonNull final OnboardingPairSenseInteractor pairSenseInteractor,
                                                            @NonNull final PreferencesInteractor preferencesInteractor) {
        return new OnboardingPairSensePresenter(interactor, userFeaturesInteractor, apiService, pairSenseInteractor,preferencesInteractor);
    }
}
