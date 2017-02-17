package is.hello.sense.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

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

    public final InteractorSubject<Notification> notificationSubject = this.subject;

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
        return null;
    }

    public void clear() {
        if(notificationSubject != null) {
            notificationSubject.forget();
        }
    }
}
