package is.hello.sense.mvp.modules;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.ExpansionsInteractor;
import is.hello.sense.ui.activities.expansions.ExpansionSettingsActivity;
import is.hello.sense.ui.fragments.expansions.ExpansionListFragment;

@Module(complete = false, injects = {
        ExpansionSettingsActivity.class,
        ExpansionListFragment.class,
})
public class ExpansionSettingsModule {

    @Provides
    @Singleton
    public ExpansionsInteractor providesExpansionListInteractor(@NonNull final ApiService apiService){
        return new ExpansionsInteractor(apiService);
    }

}
