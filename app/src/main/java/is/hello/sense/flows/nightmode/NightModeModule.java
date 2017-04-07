package is.hello.sense.flows.nightmode;

import dagger.Module;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;

@Module(complete = false,
        injects = {
                NightModeActivity.class,
                NightModeFragment.class,
                HomeActivity.class
        }
)
public class NightModeModule {

}
