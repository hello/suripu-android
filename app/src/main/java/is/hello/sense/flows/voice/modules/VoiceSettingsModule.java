package is.hello.sense.flows.voice.modules;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.flows.voice.ui.fragments.VoiceSettingsListFragment;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.DevicesInteractor;

@Module(complete = false,
        injects = {
                VoiceSettingsActivity.class,
                VoiceSettingsListFragment.class
})
public class VoiceSettingsModule {

    @Provides
    @Singleton
    CurrentSenseInteractor providesCurrentSenseInteractor(final DevicesInteractor devicesInteractor) {
        return new CurrentSenseInteractor(devicesInteractor);
    }

    @Provides
    @Singleton
    public VoiceSettingsInteractor providesVoiceSettingsInteractor(@NonNull final ApiService apiService){
        return new VoiceSettingsInteractor(apiService);
    }

}
