package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.segment.analytics.Properties;


import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.ui.fragments.HomeFragment;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.functional.Functions;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.util.Analytics;
import rx.Observable;

import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static is.hello.sense.util.Logger.info;


public class HomeActivity extends ScopedInjectionActivity
        implements FragmentNavigation,
        Alert.ActionHandler,
        OnboardingFlowProvider{
    private FragmentNavigationDelegate navigationDelegate;

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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        //todo needs testing with server
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD));
        }
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);

        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        } else {
            pushFragment(new HomeFragment(), null, false);
        }
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
            //this.tabLayout.selectSoundTab(); //todo support
        } else if (intent.hasExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(NotificationReceiver.EXTRA_NOTIFICATION_PAYLOAD));
        }

    }



    @Override
    public final void pushFragment(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public final void flowFinished(@NonNull final Fragment fragment, final int responseCode, @Nullable final Intent result) {

    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
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

    //end region

    //region Device Issues and Alerts


    //endregion

    //region Alert Action Handler

    @Override
    public void unMuteSense() {
   /*     showProgressOverlay(true);
        this.voiceSettingsInteractor.setSenseId(this.preferencesInteractor.getString(PreferencesInteractor.PAIRED_SENSE_ID,
                                                                                     VoiceSettingsInteractor.EMPTY_ID));
        track(this.voiceSettingsInteractor.setMuted(false)
                                          .subscribe(Functions.NO_OP,
                                                     e -> {
                                                         showProgressOverlay(false);
                                                         ErrorDialogFragment.presentError(this,
                                                                                          e,
                                                                                          R.string.voice_settings_update_error_title);
                                                     },
                                                     () -> showProgressOverlay(false))
             );*///todo support again
    }

    public void showProgressOverlay(final boolean show) {
        /*
        this.progressOverlay.post(() -> {
            if (show) {
                this.progressOverlay.bringToFront();
                this.spinner.startSpinning();
                this.progressOverlay.setVisibility(View.VISIBLE);
            } else {
                this.spinner.stopSpinning();
                this.progressOverlay.setVisibility(View.GONE);
            }
        });
        */ //todo support again
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
                    // this.tabLayout.selectTimelineTab(); // todo support again
                    //todo support scrolling to date.

                    break;
                }
                case Notification.PILL_BATTERY: {
                    //todo handle and pass along
                    //  this.tabLayout.selectConditionsTab(); // todo support again
                    break;
                }
                default: {
                    info(getClass().getSimpleName(), "unsupported notification type " + target);
                }
            }
        });
    }

    //endregion


    public interface ScrollUp {
        void scrollUp();
    }


}