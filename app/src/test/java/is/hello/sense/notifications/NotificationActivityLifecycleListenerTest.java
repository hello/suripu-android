package is.hello.sense.notifications;

import android.app.Activity;
import android.content.IntentFilter;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NotificationActivityLifecycleListenerTest extends InjectionTestCase {
    @Inject
    NotificationActivityLifecycleListener notificationActivityLifecycleListener;

    @Test
    public void onActivityStarted() throws Exception {
        final Activity mockActivity = mock(Activity.class);

        notificationActivityLifecycleListener.onActivityStarted(mockActivity);
        verify(mockActivity).registerReceiver(any(NotificationMessageReceiver.class),
                                              any(IntentFilter.class));
    }

    @Test
    public void onActivityStopped() throws Exception {
        final Activity mockActivity = mock(Activity.class);

        notificationActivityLifecycleListener.onActivityStopped(mockActivity);
        verify(mockActivity).unregisterReceiver(any(NotificationMessageReceiver.class));
    }

    @Test
    public void onActivityCreated() throws Exception {
        final Activity mockActivity = mock(Activity.class);
        final NotificationPressedInterceptorCounter mockCounter = mock(NotificationPressedInterceptorCounter.class);
        final NotificationActivityLifecycleListener lifecycleListener = new NotificationActivityLifecycleListener(mockCounter);
        lifecycleListener.onActivityCreated(mockActivity, null);
        verify(mockCounter).updateCounter(mockActivity, true);
    }

    @Test
    public void onActivityDestroyed() throws Exception {
        final Activity mockActivity = mock(Activity.class);
        final NotificationPressedInterceptorCounter mockCounter = mock(NotificationPressedInterceptorCounter.class);
        final NotificationActivityLifecycleListener lifecycleListener = new NotificationActivityLifecycleListener(mockCounter);
        lifecycleListener.onActivityDestroyed(mockActivity);
        verify(mockCounter).updateCounter(mockActivity, false);
    }

}