package is.hello.sense.flows.expansions.modules;

import dagger.Module;
import is.hello.sense.flows.expansions.ui.activities.ExpansionValuePickerActivity;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailPickerFragment;

@Module(complete = false,
        includes = ExpansionSettingsModule.class,
        injects = {
                ExpansionDetailPickerFragment.class,
                ExpansionValuePickerActivity.class
        })
public class ExpansionPickerModule {
}
