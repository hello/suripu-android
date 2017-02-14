package is.hello.sense.flows.home.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;


import is.hello.buruberi.util.Rx;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.ui.fragments.HomePresenterFragment;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.functional.Functions;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.util.Analytics;
import rx.Observable;

import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static is.hello.sense.util.Logger.info;


public class HomeActivity extends FragmentNavigationActivity
        implements
        OnboardingFlowProvider {

    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    private static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";

    public static Intent getIntent(@NonNull final Context context,
                                   @OnboardingActivity.Flow final int fromFlow) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(HomeActivity.EXTRA_ONBOARDING_FLOW, fromFlow);
        return intent;
    }

    @Override
    protected void onCreateAction() {
        initialize();
        pushFragment(new HomePresenterFragment(), null, false);
    }

    @Override
    protected void onReCreateAction(@NonNull final Bundle savedInstanceState) {
        super.onReCreateAction(savedInstanceState);
        initialize();
    }


    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final IntentFilter loggedOutIntent = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(getApplicationContext(), loggedOutIntent);
        bindAndSubscribe(onLogOut,
                         ignored -> finish(),
                         Functions.LOG_ERROR);

    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            final Properties properties =
                    Analytics.createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                               "ACTION_SHOW_ALARMS");
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            /// this.tabLayout.selectSoundTab(); todo support again
        } else if (intent.hasExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD));
        }

    }

    //region Onboarding flow provider

    @OnboardingActivity.Flow
    public int getOnboardingFlow() {
        @OnboardingActivity.Flow
        final int flow =
                getIntent().getIntExtra(EXTRA_ONBOARDING_FLOW,
                                        OnboardingActivity.FLOW_NONE);
        return flow;
    }
    //endregion

    //region Notifications

    private void dispatchNotification(@NonNull final Bundle notification) {
        this.stateSafeExecutor.execute(() -> {
            info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            @Notification.Type
            final String target = Notification.typeFromBundle(notification);
            switch (target) {
                case Notification.SLEEP_SCORE: {
                    // this.tabLayout.selectTimelineTab(); todo support again
                    //todo support scrolling to date.
                    break;
                }
                case Notification.PILL_BATTERY: {
                    //todo handle and pass along
                    // this.tabLayout.selectConditionsTab(); todo support again
                    break;
                }
                default: {
                    info(getClass().getSimpleName(), "unsupported notification type " + target);
                }
            }
        });
    }
    //endregion


    private void initialize() {
        //todo needs testing with server
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD));
        }
    }


    public interface ScrollUp {
        void scrollUp();
    }


}