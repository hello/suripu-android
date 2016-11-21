package is.hello.sense.flows.smartalarm.modules;

import dagger.Module;
import is.hello.sense.flows.expansions.modules.ExpansionSettingsModule;
import is.hello.sense.flows.smartalarm.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.flows.smartalarm.ui.fragments.SmartAlarmDetailFragment;

@Module(complete = false,
        includes = ExpansionSettingsModule.class,
        injects = {
                SmartAlarmDetailActivity.class,
                SmartAlarmDetailFragment.class
        })
public class SmartAlarmDetailModule {
}
