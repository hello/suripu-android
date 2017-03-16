package is.hello.sense.flows.nightmode;

import dagger.Module;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;

@Module(complete = false,
        injects = {
                NightModeActivity.class,
                NightModeFragment.class,
        }
)
public class NightModeModule {
}
