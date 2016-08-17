package is.hello.sense.onboarding;

import dagger.Module;
import is.hello.sense.SenseOTAModule;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
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
        OnboardingRegisterWeightFragment.class,
        OnboardingRegisterHeightFragment.class,
        BluetoothFragment.class,
        OnboardingRegisterAudioFragment.class,
        OnboardingUnsupportedDeviceFragment.class,
        PairSenseFragment.class,
        OnboardingPairPillFragment.class,
        OnboardingSenseColorsFragment.class,
        OnboardingRoomCheckFragment.class,

})
public class OnboardingModule {
}
