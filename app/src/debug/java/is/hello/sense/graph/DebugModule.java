package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.debug.EnvironmentActivity;
import is.hello.sense.debug.PiruPeaActivity;

@Module(complete = false,
        library = true,
        overrides = true,
        injects = {
                EnvironmentActivity.class,
                PiruPeaActivity.class,
                NonsensePresenter.class,
        })
public class DebugModule {
}
