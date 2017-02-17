package is.hello.sense.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

/**
 * Provide way to receive updates to {@link Notification}
 */

public class NotificationInteractor extends ValueInteractor<Notification> {

    @VisibleForTesting
    final InteractorSubject<Notification> notificationSubject = this.subject;

    public NotificationInteractor(@NonNull final Context context) {
        final Observable<Intent> logOut = Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOut.subscribe(ignored -> this.clear(), Functions.LOG_ERROR);
    }

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
        return Observable.just(null);
    }

    public void clear() {
        if(notificationSubject != null) {
            notificationSubject.forget();
        }
    }

    public void onNext(@NonNull final Notification notification) {
        this.notificationSubject.onNext(notification);
    }

    public Observable<Notification> filter(@NonNull @Notification.Type final String type) {
        return notificationSubject.filter( notification -> isValidTypeMatch(notification,
                                                                            type))
                .doOnNext(notification -> notification.setSeen(true));
    }

    private boolean isValidTypeMatch(@Nullable final Notification notification,
                             @NonNull @Notification.Type final String type) {
        return notification != null
                && !notification.hasSeen()
                && notification.getType().equals(type);
    }
}
