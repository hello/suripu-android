package is.hello.sense;

import dagger.Module;
import is.hello.sense.interactors.SenseOTAStatusInteractor;
import is.hello.sense.interactors.SenseVoiceInteractor;
import is.hello.sense.ui.fragments.onboarding.SenseVoiceFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAIntroFragment;

@Module(
        complete = false,
        injects = {
                SenseOTAIntroFragment.class,
                SenseOTAFragment.class,
                SenseOTAStatusInteractor.class,
                SenseVoiceFragment.class,
                SenseVoiceInteractor.class,
        }
)
public class SenseOTAModule {

}
