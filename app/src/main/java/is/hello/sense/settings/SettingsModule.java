package is.hello.sense.settings;

import dagger.Module;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.fragments.settings.ChangeEmailFragment;
import is.hello.sense.ui.fragments.settings.ChangePasswordFragment;
import is.hello.sense.ui.fragments.settings.DeviceTimeZoneFragment;
import is.hello.sense.ui.fragments.settings.NotificationsSettingsFragment;
import is.hello.sense.ui.fragments.settings.UnitSettingsFragment;

@Module(complete = false, injects = {
        AppSettingsFragment.class,
        AccountSettingsFragment.class,
        ChangePasswordFragment.class,
        ChangeEmailFragment.class,
        UnitSettingsFragment.class,
        AccountPresenter.class,
        NotificationsSettingsFragment.class,
        DeviceTimeZoneFragment.class
})
public class SettingsModule {
}
