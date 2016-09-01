package is.hello.sense;


import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.presenters.SenseUpdateIntroPresenter;
import is.hello.sense.presenters.SenseUpdateReadyPresenter;
import is.hello.sense.presenters.UnpairPillPresenter;
import is.hello.sense.presenters.UpdatePairSensePresenter;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.presenters.connectwifi.UpdateConnectWifiPresenter;
import is.hello.sense.presenters.pairpill.BasePairPillPresenter;
import is.hello.sense.presenters.pairpill.UpgradePairPillPresenter;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.UpdateSelectWifiNetworkPresenter;
import is.hello.sense.settings.SettingsWifiModule;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;
import is.hello.sense.ui.fragments.pill.UnpairPillFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateReadyFragment;
import is.hello.sense.ui.fragments.updating.UpdateConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.UpdateSelectWifiNetworkFragment;

@Module(
        complete = false,
        includes = {
                //todo include after converting fragments to use presenters
                // SenseOTAModule.class,
                SettingsWifiModule.class
        },
        injects = {
                SenseUpdateActivity.class,
                SenseUpdateIntroFragment.class,
                PairSenseFragment.class,
                SenseUpdateReadyFragment.class,
                SenseResetOriginalFragment.class,
                UnpairPillFragment.class,
                UnpairPillPresenter.class,
                PairPillFragment.class,
                BluetoothFragment.class,
                UpdateConnectToWiFiFragment.class,
                UpdateSelectWifiNetworkFragment.class
        }
)
public class SenseUpdateModule {

    @Provides
    @Singleton
    SwapSenseInteractor providesSwapSenseInteractor(final ApiService apiService) {
        return new SwapSenseInteractor(apiService);
    }

    @Provides
    @Singleton
    BaseConnectWifiPresenter provideBaseConnectWifiPresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                                             @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                                             @NonNull final ApiService apiService) {
        return new UpdateConnectWifiPresenter(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @Provides
    @Singleton
    BaseSelectWifiNetworkPresenter providesSelectWifiNetworkPresenter(final HardwareInteractor interactor) {
        return new UpdateSelectWifiNetworkPresenter(interactor);
    }

    @Provides
    @Singleton
    SenseResetOriginalInteractor providesResetOriginalSenseInteractor(final DevicesInteractor devicesInteractor){
        return new SenseResetOriginalInteractor(devicesInteractor);
    }

    @Provides
    @Singleton
    SenseResetOriginalPresenter providesSenseResetOriginalPresenter(final HardwareInteractor interactor, final SenseResetOriginalInteractor senseResetOriginalInteractor) {
        final SenseResetOriginalPresenter presenter = new SenseResetOriginalPresenter(interactor, senseResetOriginalInteractor);
        // todo interactor.setInteractorOutput(presenter);
        return presenter;
    }

    @Provides
    @Singleton
    SenseUpdateIntroPresenter providesSenseUpdateIntroPresenter() {
        return new SenseUpdateIntroPresenter();
    }

    @Provides
    @Singleton
    SenseUpdateReadyPresenter providesSenseUpdateReadyPresenter() {
        return new SenseUpdateReadyPresenter();
    }

    @Provides
    @Singleton
    PairSensePresenter providesUpdatePairSensePresenter(final HardwareInteractor interactor,
                                                        final UserFeaturesInteractor userFeaturesInteractor,
                                                        final ApiService apiService,
                                                        final SwapSenseInteractor swapSenseInteractor,
                                                        final SenseResetOriginalInteractor resetOriginalInteractor){
        return new UpdatePairSensePresenter(interactor, userFeaturesInteractor, apiService, swapSenseInteractor, resetOriginalInteractor);
    }


    @Provides
    @Singleton
    BasePairPillPresenter providesUpdatePairPillPresenter(final HardwareInteractor interactor) {
        return new UpgradePairPillPresenter(interactor);
    }

    @Provides
    @Singleton
    UnpairPillPresenter providesUnpairPillPresenter() {
        return new UnpairPillPresenter();
    }
}
