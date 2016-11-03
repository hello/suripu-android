package is.hello.sense.flows.sensordetails.modules;

import dagger.Module;
import is.hello.sense.flows.sensordetails.ui.fragments.SensorDetailFragment;
import is.hello.sense.flows.sensordetails.ui.activities.SensorDetailActivity;

@Module(
        complete = false,
        injects = {
                SensorDetailActivity.class,
                SensorDetailFragment.class,
        }
)
public class SensorDetailModule {

}