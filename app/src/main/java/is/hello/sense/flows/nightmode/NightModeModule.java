package is.hello.sense.flows.nightmode;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.fragments.HomePresenterFragment;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.flows.settings.ui.fragments.AppSettingsFragment;
import is.hello.sense.interactors.PersistentPreferencesInteractor;

@Module(complete = false,
        injects = {
                NightModeActivity.class,
                NightModeFragment.class,
                HomeActivity.class,
                HomePresenterFragment.class,
                AppSettingsFragment.class
        }
)
public class NightModeModule {


        @Provides
        @Singleton
        public NightModeInteractor providesNightModeInteractor(@NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor,
                                                               @NonNull final ApiSessionManager apiSessionManager) {
                return new NightModeInteractor(persistentPreferencesInteractor,
                                               apiSessionManager);
        }
}
