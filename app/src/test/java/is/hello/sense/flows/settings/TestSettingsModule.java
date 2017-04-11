package is.hello.sense.flows.settings;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.presenters.selectwifinetwork.SettingsSelectWifiNetworkPresenter;
import is.hello.sense.presenters.selectwifinetwork.SettingsSelectWifiNetworkPresenterTest;

@Module( complete = false,
injects = {
        SettingsSelectWifiNetworkPresenterTest.class,
})
public class TestSettingsModule {

    private final boolean shouldUseToolbar = false;

    @Provides
    @Singleton
    public SettingsSelectWifiNetworkPresenter providesSettingsSelectWifiNetworkPresenter(@NonNull final HardwareInteractor hardwareInteractor) {
        return new SettingsSelectWifiNetworkPresenter(hardwareInteractor, shouldUseToolbar);
    }
}
