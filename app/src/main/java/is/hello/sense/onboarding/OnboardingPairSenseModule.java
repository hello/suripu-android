package is.hello.sense.onboarding;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.OnboardingPairSensePresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;

@Module(
        complete = false,
        injects = {
                PairSenseFragment.class
        }
)
public class OnboardingPairSenseModule {

    @Provides
    @Singleton
    PairSensePresenter providesOnboardingPairSensePresenter(final HardwareInteractor interactor,
                                                            final UserFeaturesInteractor userFeaturesInteractor,
                                                            final ApiService apiService){
        return new OnboardingPairSensePresenter(interactor, userFeaturesInteractor, apiService);
    }
}
