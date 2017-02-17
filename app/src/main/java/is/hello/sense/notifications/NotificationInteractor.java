package is.hello.sense.notifications;

import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

/**
 * Provide way to receive updates to {@link Notification}
 */

public class NotificationInteractor extends ValueInteractor<Notification> {

    public final InteractorSubject<Notification> notificationSubject = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Notification> provideUpdateObservable() {
        return null;
    }
}
