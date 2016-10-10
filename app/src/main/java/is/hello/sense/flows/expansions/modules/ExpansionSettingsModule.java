package is.hello.sense.flows.expansions.modules;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionListFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.interactors.ExpansionsInteractor;

@Module(complete = false, injects = {
        ExpansionSettingsActivity.class,
        ExpansionListFragment.class,
        ExpansionsAuthFragment.class
})
public class ExpansionSettingsModule {

    @Provides
    @Singleton
    public ExpansionsInteractor providesExpansionListInteractor(@NonNull final ApiService apiService){
        return new ExpansionsInteractor(apiService);
    }

}
