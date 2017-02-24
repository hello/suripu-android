package is.hello.sense.notifications;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;


public class NotificationInteractorTest extends InjectionTestCase {
    @Inject
    NotificationInteractor notificationInteractor;

    @Test
    public void canUpdate() throws Exception {
        assertThat(notificationInteractor.canUpdate(), equalTo(false));
    }

    @Test
    public void provideUpdateObservable() throws Exception {
        final Notification result = Sync.last(notificationInteractor.provideUpdateObservable());
        assertNull(result);
    }

    @Test
    public void filter() throws Exception {
        final Notification incoming = new Notification(Notification.UNKNOWN, null);
        final Notification result = Sync.wrapAfter(() -> notificationInteractor.onNext(incoming),
                                                   notificationInteractor.filter(incoming.getType()))
                                        .last();

        assertThat(result, equalTo(incoming));
        assertThat(result.hasSeen(), equalTo(true));
    }

    @Test
    public void filterOutDifferentType() throws Exception {
        final Notification ignored = new Notification(Notification.UNKNOWN, null);
        final Notification matching = new Notification(Notification.SLEEP_SCORE, null);
        final Notification result = Sync.wrapAfter(() -> {
            notificationInteractor.onNext(ignored);
            notificationInteractor.onNext(matching);
        }, notificationInteractor.filter(matching.getType()))
                                        .last();

        assertThat(result, equalTo(matching));
    }

    @Test
    public void filterOutHasSeen() throws Exception {
        @Notification.Type
        final String type = Notification.UNKNOWN;
        final Notification seen = new Notification(type, null);
        seen.setSeen(true);
        final Notification unseen = new Notification(type, null);
        final Notification result = Sync.wrapAfter(() -> {
            notificationInteractor.onNext(seen);
            notificationInteractor.onNext(unseen);
        }, notificationInteractor.filter(type))
                                        .last();

        assertThat(result, equalTo(unseen));
    }

}