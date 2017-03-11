package is.hello.sense.notifications;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.flows.notification.interactors.NotificationSettingsInteractorTest;

@Module(complete = false, injects = {
        NotificationInteractorTest.class,
        NotificationSettingsInteractorTest.class,
        NotificationActivityLifecycleListenerTest.class,
        NotificationPressedInterceptorCounterTest.class,
})
public class TestNotificationModule {
    @Provides
    @Singleton
    public NotificationInteractor providesNotificationInteractor(@NonNull final Context context) {
        return new NotificationInteractor(context);
    }

    @Provides
    @Singleton
    public NotificationActivityLifecycleListener providesNotificationActivityLifecycleListener(@NonNull final NotificationPressedInterceptorCounter counter) {
        final NotificationActivityLifecycleListener listener = new NotificationActivityLifecycleListener(counter);
        return listener;
    }

    @Provides
    @Singleton
    public NotificationPressedInterceptorCounter providesNotificationPressedInterceptorCounter() {
        return new NotificationPressedInterceptorCounter();
    }
}
