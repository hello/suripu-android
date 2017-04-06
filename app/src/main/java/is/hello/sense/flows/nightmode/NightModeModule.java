package is.hello.sense.flows.nightmode;

import android.content.Context;
import android.support.annotation.NonNull;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.flows.generic.ui.interactors.LocationInteractor;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.interactors.PersistentPreferencesInteractor;

@Module(complete = false,
        injects = {
                NightModeActivity.class,
                NightModeFragment.class,
        }
)
public class NightModeModule {

    @Provides
    @Singleton
    LocationInteractor provideLocationInteractor(@NonNull final Context context,
                                                 @NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor) {
        return new LocationInteractor(context, persistentPreferencesInteractor);
    }
}
