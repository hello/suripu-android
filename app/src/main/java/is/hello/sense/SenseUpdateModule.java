package is.hello.sense;


import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.presenters.BasePairPillPresenter;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.presenters.UpdatePairPillPresenter;
import is.hello.sense.presenters.UpdatePairSensePresenter;
import is.hello.sense.settings.SettingsWifiModule;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;
import is.hello.sense.ui.fragments.pill.UnpairPillFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateReadyFragment;

@Module(
        complete = false,
        includes = {
                SenseOTAModule.class,
                SettingsWifiModule.class
        },
        injects = {
                SenseUpdateActivity.class,
                SenseUpdateIntroFragment.class,
                PairSenseFragment.class,
                SenseUpdateReadyFragment.class,
                SenseResetOriginalFragment.class,
                UnpairPillFragment.class,
                PairPillFragment.class,
                BaseHardwareFragment.class,
                BluetoothFragment.class,
                ConnectToWiFiFragment.class,
                SelectWiFiNetworkFragment.class,
        }
)
public class SenseUpdateModule {

    @Provides
    @Singleton
    SenseResetOriginalInteractor providesSenseResetOriginalInteractor(final ApiService apiService) {
        return new SenseResetOriginalInteractor(apiService);
    }

    @Provides
    @Singleton
    SenseResetOriginalPresenter providesSenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor) {
        final SenseResetOriginalPresenter presenter = new SenseResetOriginalPresenter(interactor);
        // todo interactor.setInteractorOutput(presenter);
        return presenter;
    }

    @Provides
    @Singleton
    BasePairSensePresenter providesUpdatePairSensePresenter(final HardwareInteractor interactor){
        return new UpdatePairSensePresenter(interactor);
    }

    @Provides
    @Singleton
    BasePairPillPresenter providesUpdatePairPillPresenter(){
        return new UpdatePairPillPresenter();
    }
}
