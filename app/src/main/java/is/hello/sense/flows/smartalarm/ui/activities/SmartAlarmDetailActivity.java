package is.hello.sense.flows.smartalarm.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.segment.analytics.Properties;

import org.joda.time.LocalTime;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.smartalarm.modules.SmartAlarmDetailModule;
import is.hello.sense.flows.smartalarm.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionAppCompatActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.NotTested;

public class SmartAlarmDetailActivity extends ScopedInjectionAppCompatActivity
        implements FragmentNavigation {

    //region static fields & functions
    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".EXTRA_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".EXTRA_INDEX";

    /**
     * @param context used to start the activity.
     * @param alarm   alarm object to represent.
     * @param index   position of alarm. If new should be {@link Constants#NONE}.
     */
    @NotTested
    public static void startActivity(@NonNull final Context context,
                                     @NonNull final Alarm alarm,
                                     final int index) {
        final Intent intent = new Intent(context, SmartAlarmDetailActivity.class);
        intent.putExtra(EXTRA_ALARM, alarm);
        intent.putExtra(EXTRA_INDEX, index);
        context.startActivity(intent);
    }

    /**
     * @param activity   used to start the activity.
     * @param alarm      alarm object to represent.
     * @param index      position of alarm. If new should be {@link Constants#NONE}.
     * @param resultCode result code to use.
     */
    @NotTested
    public static void startActivityForResult(@NonNull final Activity activity,
                                              @NonNull final Alarm alarm,
                                              final int index,
                                              final int resultCode) {
        final Intent intent = new Intent(activity, SmartAlarmDetailActivity.class);
        intent.putExtra(EXTRA_ALARM, alarm);
        intent.putExtra(EXTRA_INDEX, index);
        activity.startActivityForResult(intent, resultCode);
    }
    // endregion

    //region fields
    @Inject
    ApiSessionManager sessionManager;
    private FragmentNavigationDelegate navigationDelegate;
    //endregion

    //region ScopedInjectionActivity

    @NotTested
    @Override
    protected void onStart() {
        super.onStart();
        bounce();
    }

    @NotTested
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        //noinspection ConstantConditions
        //todo move to custom style
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.app_style_ab_cancel);
            actionBar.setHomeActionContentDescription(android.R.string.cancel);
            actionBar.setTitle(R.string.title_alarm);
        }
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);
        if (savedInstanceState == null) {
            if (AlarmClock.ACTION_SET_ALARM.equals(getIntent().getAction())) {
                final Properties properties = Analytics
                        .createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                          "ACTION_SET_ALARM");
                Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
                processSetAlarmIntent();
                return;
            }
            final Alarm alarm = (Alarm) getIntent().getSerializableExtra(SmartAlarmDetailActivity.EXTRA_ALARM);
            final int index = getIntent().getIntExtra(SmartAlarmDetailActivity.EXTRA_INDEX, Constants.NONE);
            showSmartAlarmDetailFragment(alarm, index, false);
        } else {
            this.navigationDelegate.onRestoreInstanceState(savedInstanceState);
        }
    }

    @NotTested
    @Override
    protected void onResume() {
        super.onResume();
        // For some reason this doesn't work if called from onCreate like it does in SensorDetailActivity.
        setStatusBarColorPrimary();
    }

    @NotTested
    @Override
    protected List<Object> getModules() {
        return Collections.singletonList(new SmartAlarmDetailModule());
    }

    @NotTested
    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (!(fragment instanceof OnBackPressedInterceptor) ||
                !((OnBackPressedInterceptor) fragment).onInterceptBackPressed(this.stateSafeExecutor.bind(super::onBackPressed))) {
            stateSafeExecutor.execute(super::onBackPressed);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region FragmentNavigation
    @NotTested
    @Override
    public final void pushFragment(@NonNull final Fragment fragment,
                                   @Nullable final String title,
                                   final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @NotTested
    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                                    @Nullable final String title,
                                                    final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @NotTested
    @Override
    public final void popFragment(@NonNull final Fragment fragment,
                                  final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @NotTested
    @Override
    public final void flowFinished(@NonNull final Fragment fragment,
                                   final int responseCode,
                                   @Nullable final Intent result) {
        switch (responseCode) {
            case RESULT_CANCELED:
                // When the user has backed out of cancelling their alarm.
                if (fragment instanceof SmartAlarmDetailFragment) {
                    finish();
                    return;
                }
                popFragment(fragment, false);
                break;
            case RESULT_OK:
                setResult(RESULT_OK);
                finish();
                break;
            default:


        }
    }

    @NotTested
    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }
    //endregion

    //region methods

    /**
     * Shows the fragment that will allow the user to create/modify/delete the alarm passed to this
     * activity.
     *
     * @param alarm  should be provided with {@link SmartAlarmDetailActivity#startActivity(Context, Alarm, int)}
     * @param index  position of alarm in list from {@link is.hello.sense.flows.home.ui.fragments.SmartAlarmListFragment}.
     *               Should be {@link Constants#NONE} if the user is creating a new one.
     * @param skipUI true if from another app saving an alarm and we should not show any UI while
     *               saving it.
     */
    @NotTested
    private void showSmartAlarmDetailFragment(@NonNull final Alarm alarm,
                                              final int index,
                                              final boolean skipUI) {
        pushFragment(SmartAlarmDetailFragment.newInstance(alarm, index, skipUI),
                     SmartAlarmDetailFragment.class.getSimpleName(),
                     false);
    }

    /**
     * Call if this activity is started from another app saving an alarm for the user.
     */
    @NotTested
    private void processSetAlarmIntent() {
        final Intent intent = getIntent();
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, Alarm.DEFAULT_HOUR);
        final int minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, Alarm.DEFAULT_MINUTE);
        final List<Integer> calendarDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);

        final Alarm alarm = new Alarm();
        alarm.setTime(new LocalTime(hour, minute));
        if (!Lists.isEmpty(calendarDays)) {
            final Set<Integer> days = alarm.getDaysOfWeek();
            for (final Integer calendarDay : calendarDays) {
                days.add(DateFormatter.calendarDayToJodaTimeDay(calendarDay));
            }
        }

        final int index = Constants.NONE;
        final boolean skipUI = (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false) &&
                intent.hasExtra(AlarmClock.EXTRA_HOUR) &&
                intent.hasExtra(AlarmClock.EXTRA_MINUTES));
        showSmartAlarmDetailFragment(alarm, index, skipUI);
    }

    /**
     * Kill this activity if the user isn't logged in.
     */
    @NotTested
    private void bounce() {
        if (!sessionManager.hasSession()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }
    }
    //endregion
}
