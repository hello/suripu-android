package is.hello.sense.flows.voice.modules;

import dagger.Module;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.flows.voice.ui.fragments.VoiceSettingsListFragment;

@Module(complete = false,
        injects = {
                VoiceSettingsActivity.class,
                VoiceSettingsListFragment.class
})
public class VoiceSettingsModule {

}
