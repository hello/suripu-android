package is.hello.sense.notifications;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.SenseApplication;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerPresenterFragment;
import is.hello.sense.flows.notification.ui.activities.NotificationActivity;
import is.hello.sense.flows.notification.ui.fragments.NotificationFragment;
import is.hello.sense.ui.fragments.onboarding.EnableNotificationFragment;

@Module(complete = false, injects = {
        SenseApplication.class,
        EnableNotificationFragment.class,
        TimelinePagerPresenterFragment.class,
        NotificationActivity.class,
        NotificationFragment.class
})
public class NotificationModule {
    @Provides
    @Singleton
    public NotificationInteractor providesNotificationInteractor(@NonNull final Context context) {
        return new NotificationInteractor(context);
    }

    @Provides
    @Singleton
    public NotificationActivityLifecycleListener providesNotificationActivityLifecycleListener() {
        return new NotificationActivityLifecycleListener();
    }
}
