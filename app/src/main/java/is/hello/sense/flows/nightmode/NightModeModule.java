package is.hello.sense.flows.nightmode;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.fragments.onboarding.SignInFragment;
import is.hello.sense.util.LocationUtil;

@Module(complete = false,
        injects = {
                NightModeActivity.class,
                NightModeFragment.class,
                SenseApplication.class,
                SignInFragment.class,
                LaunchActivity.class
        }
)
public class NightModeModule {

        @Provides
        @Singleton
        public NightModeInteractor providesNightModeInteractor(@NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor,
                                                               @NonNull final ApiSessionManager apiSessionManager,
                                                               @NonNull final Context applicationContext,
                                                               @NonNull final LocationUtil locationUtil) {
                return new NightModeInteractor(persistentPreferencesInteractor,
                                               apiSessionManager,
                                               applicationContext,
                                               locationUtil);
        }
}
