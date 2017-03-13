package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.debug_menu_ui.EnvironmentActivity;
import is.hello.sense.debug_menu_ui.PiruPeaActivity;

@Module(complete = false,
        library = true,
        overrides = true,
        injects = {
                EnvironmentActivity.class,
                PiruPeaActivity.class,
                NonsenseInteractor.class,
        })
public class DebugModule {
}
