package is.hello.sense.flows.expansions.modules;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.expansions.ui.activities.ExpansionValuePickerActivity;
import is.hello.sense.flows.expansions.ui.fragments.ConfigSelectionFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionListFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.units.UnitFormatter;

@Module(complete = false, injects = {
        ExpansionSettingsActivity.class,
        ExpansionListFragment.class,
        ExpansionsAuthFragment.class,
        ExpansionDetailFragment.class,
        ConfigSelectionFragment.class,
        ExpansionValuePickerActivity.class
})
public class ExpansionSettingsModule {

    @Provides
    @Singleton
    public ExpansionsInteractor providesExpansionListInteractor(@NonNull final ApiService apiService){
        return new ExpansionsInteractor(apiService);
    }

    @Provides
    @Singleton
    public ExpansionDetailsInteractor providesExpansionDetailsInteractor(@NonNull final ApiService apiService){
        return new ExpansionDetailsInteractor(apiService);
    }

    @Provides
    @Singleton
    public ConfigurationsInteractor providesConfigurationsInteractor(@NonNull final ApiService apiService){
        return new ConfigurationsInteractor(apiService);
    }

    @Provides
    @Singleton
    public ExpansionCategoryFormatter providesExpansionCategoryFormatter(@NonNull final UnitFormatter unitFormatter){
        return new ExpansionCategoryFormatter(unitFormatter);
    }

}
