package is.hello.sense.pill;

import dagger.Module;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.fragments.pill.ConnectPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateIntroPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateReadyPillFragment;

@Module(complete = false,
        injects = {
                PillUpdateActivity.class,
                UpdateIntroPillFragment.class,
                UpdateReadyPillFragment.class,
                ConnectPillFragment.class
        }
)
public class PillModule {
}
