package is.hello.sense.notifications;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;

public class NotificationActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    private final BroadcastReceiver foregroundNotificationReceiver;

    public NotificationActivityLifecycleListener() {
        this.foregroundNotificationReceiver = new NotificationMessageReceiver(true);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

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
            Log.e(getClass().getSimpleName(), " failed to unregister foreground notification receiver");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
