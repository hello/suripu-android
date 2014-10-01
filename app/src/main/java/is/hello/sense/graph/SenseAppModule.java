package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.api.ApiModule;
import is.hello.sense.ui.HomeActivity;

@Module(
    includes = ApiModule.class,
    injects = {
        HomeActivity.class
    }
)
public class SenseAppModule {
}
