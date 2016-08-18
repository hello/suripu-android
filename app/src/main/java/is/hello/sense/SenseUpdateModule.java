package is.hello.sense;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.onboarding.PairPillFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateReadyFragment;

@Module(
        complete = false,
        includes = {
                SenseOTAModule.class
        },
        injects = {
                SenseUpdateActivity.class,
                SenseUpdateIntroFragment.class,
                PairSenseFragment.class,
                SenseUpdateReadyFragment.class,
                SenseResetOriginalFragment.class,
                PairPillFragment.class
        }
)
public class SenseUpdateModule {

    @Provides @Singleton
    SenseResetOriginalInteractor providesSenseResetOriginalInteractor(final ApiService apiService){
        return new SenseResetOriginalInteractor(apiService);
    }

    @Provides @Singleton
    SenseResetOriginalPresenter providesSenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor){
        final SenseResetOriginalPresenter presenter = new SenseResetOriginalPresenter(interactor);
        // todo interactor.setInteractorOutput(presenter);
        return presenter;
    }
}
