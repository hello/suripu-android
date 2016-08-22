package is.hello.sense.onboarding;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.SenseOTAModule;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.presenters.OnboardingPairSensePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;

@Module(complete = false,
        includes = {
                SenseOTAModule.class
        },
        injects = {
                OnboardingActivity.class,
                OnboardingUnsupportedDeviceFragment.class,
                OnboardingRegisterWeightFragment.class,
                OnboardingRegisterHeightFragment.class,
                BluetoothFragment.class,
                OnboardingRegisterAudioFragment.class,
                PairSenseFragment.class,
                OnboardingSenseColorsFragment.class,
                OnboardingRoomCheckFragment.class,

        })
public class OnboardingModule {

        @Provides
        @Singleton
        BasePairSensePresenter providesOnboardingPairSensePresenter(final HardwarePresenter hardwarePresenter){
                return new OnboardingPairSensePresenter(hardwarePresenter);
        }
}
