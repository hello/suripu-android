package is.hello.sense.mvp.modules;

import dagger.Module;
import is.hello.sense.mvp.presenters.SensorDetailFragment;
import is.hello.sense.ui.activities.SensorDetailActivity;

@Module(
        complete = false,
        injects = {
                SensorDetailActivity.class,
                SensorDetailFragment.class,
        }
)
public class SensorDetailModule {

}