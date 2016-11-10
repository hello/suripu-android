package is.hello.sense.flows.smartalarm.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.segment.analytics.Properties;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.smartalarm.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmDetailActivity extends ScopedInjectionActivity
        implements FragmentNavigation {

    //region static fields & functions
    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".ARG_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".ARG_INDEX";
    private static final String KEY_INDEX = SmartAlarmDetailActivity.class.getName() + ".KEY_INDEX";
    private static final String KEY_SKIP = SmartAlarmDetailActivity.class.getName() + ".KEY_SKIP";

    public static void startActivity(@NonNull final Context context,
                                     @NonNull final Alarm alarm,
                                     final int index) {
        final Intent intent = new Intent(context, SmartAlarmDetailActivity.class);
        intent.putExtra(EXTRA_ALARM, alarm);
        intent.putExtra(EXTRA_INDEX, index);
        context.startActivity(intent);
    }
    // endregion

    //region fields
    @Inject
    ApiSessionManager sessionManager;
    private FragmentNavigationDelegate navigationDelegate;
    private Alarm alarm;
    private int index;
    private boolean skipUI = false;
    private SmartAlarmDetailFragment detailFragment;
    //endregion

    //region ScopedInjectionActivity
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(SmartAlarmDetailActivity.EXTRA_ALARM, alarm);
        outState.putInt(KEY_INDEX, index);
        outState.putBoolean(KEY_SKIP, skipUI);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
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
            } else {
                this.alarm = (Alarm) getIntent().getSerializableExtra(SmartAlarmDetailActivity.EXTRA_ALARM);
                this.index = getIntent().getIntExtra(SmartAlarmDetailActivity.EXTRA_INDEX, Constants.NONE);
            }
        } else {
            this.navigationDelegate.onRestoreInstanceState(savedInstanceState);
            this.alarm = (Alarm) savedInstanceState.getSerializable(SmartAlarmDetailActivity.EXTRA_ALARM);
            this.index = savedInstanceState.getInt(KEY_INDEX, Constants.NONE);
            this.skipUI = savedInstanceState.getBoolean(KEY_SKIP, false);
        }

        if (this.alarm == null) {
            this.alarm = new Alarm();
        }

        //noinspection ConstantConditions
        getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_cancel);
        getActionBar().setHomeActionContentDescription(android.R.string.cancel);
        getActionBar().setTitle(R.string.title_alarm);
        showSmartAlarmDetailFragment();
    }

    @Override
    protected List<Object> getModules() {
        return new ArrayList<>();
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (!(fragment instanceof OnBackPressedInterceptor) ||
                !((OnBackPressedInterceptor) fragment).onInterceptBackPressed(this.stateSafeExecutor.bind(super::onBackPressed))) {
            stateSafeExecutor.execute(super::onBackPressed);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, @NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.item_save) {
            //   detailFragment.saveAlarm();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    //endregion

    //region FragmentNavigation
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
    //endregion

    //region methods

    @NonNull
    public Alarm getAlarm() {
        return alarm;
    }

    public int getIndex() {
        return index;
    }

    public boolean skipUI() {
        return skipUI;
    }

    private void showSmartAlarmDetailFragment() {
        navigationDelegate.pushFragment(new SmartAlarmDetailFragment(), null, false);
    }

    private void processSetAlarmIntent() {
        final Intent intent = getIntent();
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 7);
        final int minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 30);
        final List<Integer> calendarDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);

        this.alarm = new Alarm();
        alarm.setTime(new LocalTime(hour, minute));
        if (!Lists.isEmpty(calendarDays)) {
            final Set<Integer> days = alarm.getDaysOfWeek();
            for (final Integer calendarDay : calendarDays) {
                days.add(DateFormatter.calendarDayToJodaTimeDay(calendarDay));
            }
        }

        this.index = Constants.NONE;
        this.skipUI = (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false) &&
                intent.hasExtra(AlarmClock.EXTRA_HOUR) &&
                intent.hasExtra(AlarmClock.EXTRA_MINUTES));
    }

    private void bounce() {
        if (!sessionManager.hasSession()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }
    }
    //endregion
}
