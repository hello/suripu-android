package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.api.ApiModule;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.TemporaryOnboardingFragment;
import is.hello.sense.ui.fragments.TimelineFragment;

@Module(
    includes = {ApiModule.class},
    injects = {
        LaunchActivity.class,
        HomeActivity.class,
        OnboardingActivity.class,
        TemporaryOnboardingFragment.class,
        TimelineFragment.class,
    }
)
public class SenseAppModule {
}
