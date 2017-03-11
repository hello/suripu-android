package is.hello.sense.notifications;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * Keep track of number of active Activities that implement {@link OnNotificationPressedInterceptor}
 */

public class NotificationPressedInterceptorCounter {
    private int count;

    NotificationPressedInterceptorCounter() {
        this.count = 0;
    }

    public boolean hasActiveInterceptors() {
        return count > 0;
    }

    void updateCounter(@NonNull final Activity activity, final boolean increment) {
        if (activity instanceof OnNotificationPressedInterceptor) {
            count += increment ? 1 : -1;
        }
    }
}
