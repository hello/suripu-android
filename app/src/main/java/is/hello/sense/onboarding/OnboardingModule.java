package is.hello.sense.onboarding;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.presenters.OnboardingPairSensePresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.presenters.pairpill.BasePairPillPresenter;
import is.hello.sense.presenters.pairpill.UpgradePairPillPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterWeightFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;

@Module(complete = false,
        library = true,
        includes = {
                //todo include after converting fragments to use presenters
                // SenseOTAModule.class,
        },
        injects = {
                OnboardingActivity.class,
                OnboardingUnsupportedDeviceFragment.class,
                RegisterWeightFragment.class,
                RegisterHeightFragment.class,
                BluetoothFragment.class,
                OnboardingRegisterAudioFragment.class,
                PairSenseFragment.class,
                PairPillFragment.class,
                OnboardingSenseColorsFragment.class,
                OnboardingRoomCheckFragment.class,
        }
)
public class OnboardingModule {

    @Provides
    @Singleton
    PairSensePresenter providesOnboardingPairSensePresenter(final HardwareInteractor interactor,
                                                            final UserFeaturesInteractor userFeaturesInteractor,
                                                            final ApiService apiService,
                                                            final PairSenseInteractor pairSenseInteractor) {
        return new OnboardingPairSensePresenter(interactor, userFeaturesInteractor, apiService, pairSenseInteractor);
    }

    @Provides
    @Singleton
    BasePairPillPresenter providesOnboardingPairPillPresenter(final HardwareInteractor interactor) {
        return new UpgradePairPillPresenter(interactor);
    }
}
