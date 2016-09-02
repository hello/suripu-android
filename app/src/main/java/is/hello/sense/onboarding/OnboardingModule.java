package is.hello.sense.onboarding;

import dagger.Module;
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

}
