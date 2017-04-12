package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.generic.ui.interactors.LocationInteractor;
import is.hello.sense.flows.home.ui.fragments.HomePresenterFragment;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.notifications.Notification;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.util.Analytics;
import rx.Observable;

import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static is.hello.sense.util.Logger.info;


public class HomeActivity extends FragmentNavigationActivity
        implements
        OnboardingFlowProvider {

    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    private static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";

    @Inject
    NightModeInteractor nightModeInteractor;
    @Inject
    LocationInteractor locationInteractor;


    public static Intent getIntent(@NonNull final Context context,
                                   @OnboardingActivity.Flow final int fromFlow) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(HomeActivity.EXTRA_ONBOARDING_FLOW, fromFlow);
        return intent;
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationInteractor.stop();
    }

    @Override
    protected void onCreate(@NonNull final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int initialConfigMode = nightModeInteractor.getConfigMode(getResources());
        bindAndSubscribe(nightModeInteractor.currentNightMode,
                         mode -> {
                             if (nightModeInteractor.requiresRecreate(initialConfigMode,
                                                                      getResources())) {
                                 recreate();
                             }
                         },
                         Functions.LOG_ERROR);
    }

    @Override
    protected void onCreateAction() {
        if (AppCompatDelegate.MODE_NIGHT_AUTO == this.nightModeInteractor.getCurrentMode()) {
            this.locationInteractor.start();
        }
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD));
        }
        pushFragment(new HomePresenterFragment(), null, false);
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
            final HomePresenterFragment fragment = getHomePresenterFragment();
            if (fragment != null) {
                fragment.selectSoundTab();
            }
        } else if (intent.hasExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD));
        }
    }

    @Override
    public void recreate() {
        super.recreate();
        setStatusBarColorRes(R.color.status_bar_normal);
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

    private void dispatchNotification(@NonNull final Bundle bundle) {
        this.stateSafeExecutor.execute(() -> {
            final Notification notification = Notification.fromBundle(bundle);
            Analytics.trackEvent(Analytics.Notification.EVENT_OPEN,
                                 Analytics.createProperties(Analytics.Notification.PROP_TYPE, notification.getType(),
                                                            Analytics.Notification.PROP_DETAIL, notification.getDetail()));

            final HomePresenterFragment fragment = getHomePresenterFragment();
            if (fragment == null) {
                return;
            }
            fragment.forwardNotification(notification);
            switch (notification.getType()) {
                case Notification.SLEEP_SCORE: {
                    fragment.selectTimelineTab();
                    break;
                }
                case Notification.SYSTEM: {
                    dispatchSystemDetailNotification(
                            Notification.systemTypeFromString(notification.getDetail()));
                    break;
                }
                default: {
                    info(getClass().getSimpleName(), "unsupported notification type " + notification.getType());
                }
            }
        });
    }

    private void dispatchSystemDetailNotification(@NonNull
                                                  @Notification.SystemType
                                                  final String systemType) {
        switch (systemType) {
            case Notification.PILL_BATTERY: {
                DeviceListFragment.startStandaloneFrom(this);
                break;
            }
            default: {
                info(getClass().getSimpleName(), "unsupported notification detail " + systemType);
            }
        }
    }

    //endregion


    @Nullable
    private HomePresenterFragment getHomePresenterFragment() {
        final Fragment fragment = getTopFragment();
        if (fragment instanceof HomePresenterFragment) {
            return (HomePresenterFragment) fragment;
        }
        return null;
    }


    public interface ScrollUp {
        void scrollUp();
    }


}