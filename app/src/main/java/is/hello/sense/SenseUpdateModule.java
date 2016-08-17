package is.hello.sense;


import dagger.Module;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.onboarding.PairPillFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateReadyFragment;

@Module(
        complete = false,
        includes = {
                SenseOTAModule.class
        },
        injects = {
                SenseUpdateActivity.class,
                SenseUpdateIntroFragment.class,
                PairSenseFragment.class,
                SenseUpdateReadyFragment.class,
                SenseResetOriginalFragment.class,
                PairPillFragment.class
        }
)
public class SenseUpdateModule {
}
