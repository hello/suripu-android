package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.debug.EnvironmentActivity;
import is.hello.sense.debug.PiruPeaActivity;
import is.hello.sense.debug.WelcomeDialogsActivity;

@Module(complete = false,
        library = true,
        overrides = true,
        injects = {
                EnvironmentActivity.class,
                PiruPeaActivity.class,
                NonsenseInteractor.class,
                WelcomeDialogsActivity.class
        })
public class DebugModule {
}
