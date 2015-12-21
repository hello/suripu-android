package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.debug.PiruPeaActivity;

@Module(complete = false,
        library = true,
        overrides = true,
        injects = {
                PiruPeaActivity.class,
        })
public class DebugModule {
}
