package is.hello.sense.onboarding;

import dagger.Module;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.onboarding.ConnectPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.UpdateReadyPillFragment;

@Module(complete = false, injects = {
        OnboardingActivity.class,
        OnboardingRegisterWeightFragment.class,
        OnboardingRegisterHeightFragment.class,
        OnboardingBluetoothFragment.class,
        OnboardingRegisterAudioFragment.class,
        OnboardingUnsupportedDeviceFragment.class,
        OnboardingPairSenseFragment.class,
        OnboardingPairPillFragment.class,
        ConnectPillFragment.class,
        UpdateReadyPillFragment.class,
        OnboardingSenseColorsFragment.class,
        OnboardingRoomCheckFragment.class,

})
public class OnboardingModule {
}
