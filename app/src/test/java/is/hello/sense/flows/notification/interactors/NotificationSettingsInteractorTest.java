package is.hello.sense.flows.notification.interactors;

import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class NotificationSettingsInteractorTest extends InjectionTestCase {
    @Inject
    NotificationSettingsInteractor notificationSettingsInteractor;

    static NotificationSetting create(final boolean enabled) {
        return new NotificationSetting(null,
                                       null,
                                       enabled,
                                       null);
    }

    @Test
    public void enableAll() throws Exception {
        final ArrayList<NotificationSetting> list = new ArrayList<>(2);
        list.add(create(false));
        list.add(create(false));
        list.add(create(false));

        notificationSettingsInteractor.notificationSettings.onNext(list);

        Sync.last(notificationSettingsInteractor.enableAll());

        for(final NotificationSetting notificationSetting : list) {
            assertThat(notificationSetting.isEnabled(), equalTo(true));
        }
    }

    @Test
    public void disableAll() throws Exception {
        final ArrayList<NotificationSetting> list = new ArrayList<>(1);
        list.add(create(true));
        list.add(create(true));
        list.add(create(true));

        notificationSettingsInteractor.notificationSettings.onNext(list);

        Sync.last(notificationSettingsInteractor.disableAll());

        for(final NotificationSetting notificationSetting : list) {
            assertThat(notificationSetting.isEnabled(), equalTo(false));
        }
    }

    @Test
    public void updateIfInvalid() throws Exception {
        notificationSettingsInteractor.notificationSettings.forget();
        notificationSettingsInteractor.notificationSettings.onNext(null);
        final boolean updated = notificationSettingsInteractor.updateIfInvalid();
        assertThat(updated, equalTo(true));
        assertThat(notificationSettingsInteractor.notificationSettings.hasValue(), equalTo(true));
    }

    @Test
    public void throwInvalidException() throws Exception {
        notificationSettingsInteractor.notificationSettings.onNext(null);

        Sync.wrap(notificationSettingsInteractor.enableAll())
            .assertThrows(NotificationSettingsInteractor.InvalidException.class);

        notificationSettingsInteractor.notificationSettings.onNext(new ArrayList<>());

        Sync.wrap(notificationSettingsInteractor.enableAll())
            .assertThrows(NotificationSettingsInteractor.InvalidException.class);
    }

}