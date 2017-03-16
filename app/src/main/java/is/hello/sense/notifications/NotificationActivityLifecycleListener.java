package is.hello.sense.notifications;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

public class NotificationActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    private final BroadcastReceiver foregroundNotificationReceiver;
    private final NotificationPressedInterceptorCounter counter;

    public NotificationActivityLifecycleListener(@NonNull final NotificationPressedInterceptorCounter counter) {
        this.foregroundNotificationReceiver = new NotificationMessageReceiver(true);
        this.counter = counter;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        this.counter.updateCounter(activity, true);
    }

    @Override
    public void onActivityStarted(final Activity activity) {
        activity.registerReceiver(foregroundNotificationReceiver,
                                  NotificationMessageReceiver.getMainPriorityFilter());
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(final Activity activity) {
        try {
            activity.unregisterReceiver(foregroundNotificationReceiver);
        } catch (final IllegalArgumentException e) {
            Log.w(getClass().getSimpleName(), " failed to unregister foreground notification receiver");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        this.counter.updateCounter(activity, false);
    }
}
