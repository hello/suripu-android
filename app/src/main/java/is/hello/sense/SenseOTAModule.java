package is.hello.sense;

import dagger.Module;
import is.hello.sense.graph.presenters.SenseOTAStatusPresenter;
import is.hello.sense.graph.presenters.SenseVoicePresenter;
import is.hello.sense.ui.fragments.onboarding.SenseVoiceFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAIntroFragment;

@Module(
        complete = false,
        injects = {
                SenseOTAIntroFragment.class,
                SenseOTAFragment.class,
                SenseOTAStatusPresenter.class,
                SenseVoiceFragment.class,
                SenseVoicePresenter.class,
        }
)
public class SenseOTAModule {
}
