package is.hello.sense.notifications;

import android.app.Activity;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.activities.SenseUpgradeActivity;
import is.hello.sense.ui.activities.SettingsActivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

public class NotificationPressedInterceptorCounterTest extends InjectionTestCase {

    @Inject
    NotificationPressedInterceptorCounter notificationPressedInterceptorCounter;

    @Test
    public void updateCounter() throws Exception {
        final Activity mockInterceptedActivity = mock(TestActivity.class);
        final Activity mockNormalActivity = mock(Activity.class);

        notificationPressedInterceptorCounter.updateCounter(mockNormalActivity, true);
        assertThat(notificationPressedInterceptorCounter.hasActiveInterceptors(), equalTo(false));

        notificationPressedInterceptorCounter.updateCounter(mockInterceptedActivity, true);
        assertThat(notificationPressedInterceptorCounter.hasActiveInterceptors(), equalTo(true));

        notificationPressedInterceptorCounter.updateCounter(mockInterceptedActivity, false);
        assertThat(notificationPressedInterceptorCounter.hasActiveInterceptors(), equalTo(false));
    }

    @Test
    public void updateCounterForAppActivities() throws Exception {
        final List<Class> notificationInterceptorList = Arrays.asList(SenseUpgradeActivity.class,
                                                                      PillUpdateActivity.class,
                                                                      SettingsActivity.class,
                                                                      OnboardingActivity.class
                                                                     );

        for (final Class<Activity> interceptorActivityClass : notificationInterceptorList) {
            final Activity mockInterceptedActivity = mock(interceptorActivityClass);
            notificationPressedInterceptorCounter.updateCounter(mockInterceptedActivity, true);
            assertThat(notificationPressedInterceptorCounter.hasActiveInterceptors(), equalTo(true));

            notificationPressedInterceptorCounter.updateCounter(mockInterceptedActivity, false);
            assertThat(notificationPressedInterceptorCounter.hasActiveInterceptors(), equalTo(false));
        }
    }



    class TestActivity extends Activity implements OnNotificationPressedInterceptor {
    }
}