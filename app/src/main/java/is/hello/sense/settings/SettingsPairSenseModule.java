package is.hello.sense.settings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.presenters.SettingsPairSensePresenter;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;

@Module(
        complete = false,
        injects = {
                PairSenseFragment.class
        }
)
public class SettingsPairSenseModule {

    @Provides
    @Singleton
    BasePairSensePresenter providesSettingsPairSensePresenter(final HardwareInteractor hardwareInteractor,
                                                              final UserFeaturesInteractor userFeaturesInteractor,
                                                              final ApiService apiService) {
        return new SettingsPairSensePresenter(hardwareInteractor, userFeaturesInteractor, apiService);
    }
}
