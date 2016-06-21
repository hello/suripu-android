package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.segment.analytics.Properties;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.graph.presenters.UnreadStatePresenter;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.fragments.BacksideFragment;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.util.InteractiveAnimator;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.InternalPrefManager;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.go99.Anime.isAnimating;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class HomeActivity extends ScopedInjectionActivity
        implements SlidingLayersView.Listener,
        ZoomedOutTimelineFragment.OnTimelineDateSelectedListener,
        AnimatorContext.Scene,
        ViewPager.OnPageChangeListener,
        TimelineInfoFragment.AnchorProvider,
        InsightInfoFragment.ParentProvider {
    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    public static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";

    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Inject ApiService apiService;
    @Inject DeviceIssuesPresenter deviceIssuesPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject UnreadStatePresenter unreadStatePresenter;
    @Inject LocalUsageTracker localUsageTracker;

    private long lastUpdated = System.currentTimeMillis();

    private RelativeLayout rootContainer;
    private FrameLayout backsideContainer;
    private SlidingLayersView slidingLayersView;

    private ViewPager viewPager;
    private TimelineFragmentAdapter viewPagerAdapter;
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;
    private ImageButton smartAlarmButton;

    private View progressOverlay;
    private boolean isFirstActivityRun;
    private boolean showBackside;

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());
    private final BroadcastReceiver onTimeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final LocalDate newToday = DateFormatter.todayForTimeline();
            final LocalDate selectedDate = viewPagerAdapter.getItemDate(viewPager.getCurrentItem());
            if (newToday.isBefore(selectedDate)) {
                // ViewPager does not correctly shrink when the number of items in it
                // decrease, so we have to clear its adapter, update the adapter, then
                // re-set the adapter for the update to work correctly.
                viewPager.setAdapter(null);
                viewPagerAdapter.setLatestDate(newToday);
                viewPager.setAdapter(viewPagerAdapter);
                viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), false);
            } else {
                viewPagerAdapter.setLatestDate(newToday);

                final TimelineFragment currentFragment =
                        (TimelineFragment) viewPagerAdapter.getCurrentFragment();
                if (currentFragment != null) {
                    currentFragment.updateTitle();
                }
            }
        }
    };


    //region Lifecycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        deviceIssuesPresenter.bindScope(this);
        presenterContainer.addPresenter(deviceIssuesPresenter);

        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.showBackside = false;
            this.lastUpdated = savedInstanceState.getLong("lastUpdated");
            presenterContainer.onRestoreState(savedInstanceState);
        } else {
            this.showBackside = (getOnboardingFlow() == OnboardingActivity.FLOW_SIGN_IN);

            if (NotificationRegistration.shouldRegister(this)) {
                new NotificationRegistration(this).register();
            }

            if (getIntent().hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
                dispatchNotification(getIntent().getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), false);
            }

            if (!showBackside) {
                unreadStatePresenter.update();
            }
        }

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(getIntent().getAction())) {
            final Properties properties =
                    Analytics.createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                               "ACTION_SHOW_ALARMS");
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            stateSafeExecutor.execute(() -> showBacksideWithItem(BacksideFragment.ITEM_SOUNDS, false));
        }

        deviceIssuesPresenter.update();


        this.rootContainer = (RelativeLayout) findViewById(R.id.activity_home_container);


        this.smartAlarmButton = (ImageButton) findViewById(R.id.fragment_timeline_smart_alarm);
        Views.setSafeOnClickListener(smartAlarmButton, ignored -> showBacksideWithItem(BacksideFragment.ITEM_SOUNDS, true));

        this.viewPager = (ViewPager) findViewById(R.id.activity_home_view_pager);
        viewPager.addOnPageChangeListener(this);

        this.viewPagerAdapter = new TimelineFragmentAdapter(getFragmentManager(),
                                                            preferences.getAccountCreationDate());
        viewPager.setAdapter(viewPagerAdapter);

        if (viewPager.getCurrentItem() == 0) {
            jumpToLastNight(false);
        }


        this.backsideContainer = (FrameLayout) findViewById(R.id.activity_home_backside_container);

        this.slidingLayersView = (SlidingLayersView) findViewById(R.id.activity_home_sliding_layers);
        slidingLayersView.setListener(this);
        slidingLayersView.setInteractiveAnimator(new BacksideAnimator());
        slidingLayersView.setAnimatorContext(getAnimatorContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            slidingLayersView.setBackgroundColor(ContextCompat.getColor(this, R.color.status_bar_grey));
        }


        final Fragment navigatorFragment =
                getFragmentManager().findFragmentByTag(ZoomedOutTimelineFragment.TAG);
        if (navigatorFragment != null) {
            getFragmentManager().popBackStack(ZoomedOutTimelineFragment.TAG,
                                              FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        registerReceiver(onTimeChanged, new IntentFilter(Intent.ACTION_TIME_CHANGED));
        this.progressOverlay = findViewById(R.id.activity_home_progress_overlay);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            final Properties properties =
                    Analytics.createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                               "ACTION_SHOW_ALARMS");
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            showBacksideWithItem(BacksideFragment.ITEM_SOUNDS, false);
        } else if (intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), isResumed);
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final IntentFilter loggedOutIntent = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(getApplicationContext(),
                                                                  loggedOutIntent);
        bindAndSubscribe(onLogOut,
                         ignored -> {
                             startActivity(new Intent(this, LaunchActivity.class));
                             finish();
                         },
                         Functions.LOG_ERROR);

        if (isFirstActivityRun && getOnboardingFlow() == OnboardingActivity.FLOW_NONE) {
            bindAndSubscribe(deviceIssuesPresenter.latest(),
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }

        checkInForUpdates();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("lastUpdated", lastUpdated);
        presenterContainer.onSaveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Distribution.checkForUpdates(this);

        if (showBackside) {
            slidingLayersView.openWithoutAnimation();
            this.showBackside = false;
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS) {
            if (isCurrentFragmentLastNight()) {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, reloading.");
                final TimelineFragment fragment =
                        (TimelineFragment) viewPagerAdapter.getCurrentFragment();
                if (fragment != null) {
                    fragment.update();
                }
            } else {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
                viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), false);
            }

            this.lastUpdated = System.currentTimeMillis();

            final Fragment navigatorFragment =
                    getFragmentManager().findFragmentByTag(ZoomedOutTimelineFragment.TAG);
            if (navigatorFragment != null) {
                getFragmentManager().popBackStack();
            }

            unreadStatePresenter.update();
        }

        presenterContainer.onContainerResumed();
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onTimeChanged);
        viewPager.removeOnPageChangeListener(this);

        if (isFinishing()) {
            presenterContainer.onContainerDestroyed();
        }
    }

    //endregion


    //region Notifications

    private void dispatchNotification(@NonNull final Bundle notification, final boolean animate) {
        stateSafeExecutor.execute(() -> {
            Logger.info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            final Notification target = Notification.fromBundle(notification);
            switch (target) {
                case TIMELINE: {
                    if (slidingLayersView.isOpen()) {
                        slidingLayersView.close();
                    }

                    final LocalDate date = Notification.getDate(notification);
                    final int position = viewPagerAdapter.getDatePosition(date);
                    viewPager.setCurrentItem(position, false);

                    break;
                }
                case SENSOR: {
                    showBacksideWithItem(BacksideFragment.ITEM_ROOM_CONDITIONS, animate);

                    final Intent sensorHistory = new Intent(this, SensorHistoryActivity.class);
                    final String sensorName = Notification.getSensorName(notification);
                    sensorHistory.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorName);
                    startActivity(sensorHistory);

                    break;
                }
                case TRENDS: {
                    showBacksideWithItem(BacksideFragment.ITEM_TRENDS, animate);
                    break;
                }
                case ALARM: {
                    showBacksideWithItem(BacksideFragment.ITEM_SOUNDS, animate);
                    break;
                }
                case SETTINGS: {
                    showBacksideWithItem(BacksideFragment.ITEM_APP_SETTINGS, animate);
                    break;
                }
                case INSIGHTS: {
                    showBacksideWithItem(BacksideFragment.ITEM_INSIGHTS, animate);
                    break;
                }
            }
        });
    }

    //endregion


    @Override
    public void onBackPressed() {
        if (progressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            if (slidingLayersView.isOpen()) {
                if (!slidingLayersView.isInMotion()) {
                    final BacksideFragment backsideFragment = getBacksideFragment();
                    if (backsideFragment == null || !backsideFragment.onBackPressed()) {
                        slidingLayersView.close();
                    }
                }

                return;
            } else if (!isCurrentFragmentLastNight()) {
                jumpToLastNight(true);

                return;
            }
        }

        super.onBackPressed();
    }

    public @OnboardingActivity.Flow int getOnboardingFlow() {
        final @OnboardingActivity.Flow int flow =
                getIntent().getIntExtra(EXTRA_ONBOARDING_FLOW,
                                        OnboardingActivity.FLOW_NONE);
        return flow;
    }

    public boolean isBacksideOpen() {
        return (slidingLayersView.isOpen() ||
                slidingLayersView.isInMotion());
    }

    public void setChromeTranslationAmount(final float translationAmount) {
        slidingLayersView.setTopExtraTranslationAmount(translationAmount);
    }

    public boolean isCurrentFragmentLastNight() {
        final TimelineFragment currentFragment =
                (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }

    public void jumpToLastNight(final boolean animate) {
        viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), animate);
    }


    @Override
    public @NonNull AnimatorContext getAnimatorContext() {
        return animatorContext;
    }

    public void checkInForUpdates() {
        bindAndSubscribe(apiService.checkInForUpdates(new UpdateCheckIn()),
                         response -> {
                             if (response.isNewVersion()) {
                                 final AppUpdateDialogFragment dialogFragment =
                                         AppUpdateDialogFragment.newInstance(response);
                                 dialogFragment.show(getFragmentManager(), AppUpdateDialogFragment.TAG);
                             }
                         },
                         e -> Logger.error(HomeActivity.class.getSimpleName(), "Could not run update check in", e));
    }


    //region Fragment Adapter

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int position) {
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        if (lastPagerScrollState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.beginAnimation("Timeline swipe");

            final TimelineFragment currentFragment =
                    (TimelineFragment) viewPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.onSwipeBetweenDatesStarted();
            }
        } else if (lastPagerScrollState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.endAnimation("Timeline swipe");

            Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_SWIPE, null);
        }
        this.lastPagerScrollState = state;
    }

    //endregion


    //region Shared Chrome

    @Nullable
    @Override
    public View findAnimationAnchorView(@IdRes final int viewId) {
        final Fragment currentFragment = viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null && currentFragment.getView() != null) {
            return currentFragment.getView().findViewById(viewId);
        }

        return null;
    }

    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final BacksideFragment backsideFragment = getBacksideFragment();
        if (backsideFragment != null) {
            final BacksideTabFragment currentTabFragment = backsideFragment.getCurrentTabFragment();
            if (currentTabFragment instanceof InsightInfoFragment.Parent) {
                return (InsightInfoFragment.Parent) currentTabFragment;
            }
        }

        return null;
    }

    public void hideAlarmShortcut() {
        if (smartAlarmButton.getVisibility() == View.VISIBLE && !isAnimating(smartAlarmButton)) {
            final int contentHeight = rootContainer.getMeasuredHeight();

            animatorFor(smartAlarmButton, animatorContext)
                    .translationY(contentHeight - smartAlarmButton.getTop())
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            smartAlarmButton.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }
    }

    public void showAlarmShortcut() {
        if (smartAlarmButton.getVisibility() == View.INVISIBLE && !isAnimating(smartAlarmButton)) {
            smartAlarmButton.setVisibility(View.VISIBLE);

            animatorFor(smartAlarmButton, animatorContext)
                    .translationY(0f)
                    .start();
        }
    }

    //endregion


    //region Device Issues

    public void bindDeviceIssue(@NonNull final DeviceIssuesPresenter.Issue issue) {
        if (issue == DeviceIssuesPresenter.Issue.NONE ||
                getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null) {
            return;
        }

        localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);

        final DeviceIssueDialogFragment deviceIssueDialogFragment =
                DeviceIssueDialogFragment.newInstance(issue);
        deviceIssueDialogFragment.showAllowingStateLoss(getFragmentManager(),
                                                        DeviceIssueDialogFragment.TAG);

        deviceIssuesPresenter.updateLastShown(issue);
    }

    //endregion


    //region Sliding Layers

    private @Nullable BacksideFragment getBacksideFragment() {
        return (BacksideFragment) getFragmentManager().findFragmentById(R.id.activity_home_backside_container);
    }

    @Override
    public void onTopViewWillSlideDown() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_OPENED, null);

        if (isResumed && getFragmentManager().findFragmentById(R.id.activity_home_backside_container) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_home_backside_container, new BacksideFragment())
                    .commit();
        }

        final TimelineFragment currentFragment =
                (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onTopViewWillSlideDown();
        }

        this.isFirstActivityRun = false;
    }

    @Override
    public void onTopViewDidSlideUp() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_CLOSED, null);

        final BacksideFragment backside = getBacksideFragment();
        if (backside != null) {
            stateSafeExecutor.execute(() -> getFragmentManager()
                    .beginTransaction()
                    .remove(backside)
                    .commit());
        }

        final TimelineFragment currentFragment =
                (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onTopViewDidSlideUp();
        }
    }

    public void showBacksideWithItem(final int item, final boolean animate) {
        if (slidingLayersView.isOpen()) {
            final BacksideFragment backside = getBacksideFragment();
            if (backside != null) {
                backside.setCurrentItem(item, BacksideFragment.OPTION_ANIMATE);
            }
        } else {
            InternalPrefManager.saveCurrentItem(this, item);
            if (animate) {
                slidingLayersView.open();
            } else {
                slidingLayersView.openWithoutAnimation();
            }
        }
    }

    public void toggleBacksideOpen() {
        slidingLayersView.toggle();
    }

    private class BacksideAnimator implements InteractiveAnimator {
        private final float MIN_SCALE = 0.85f;
        private final float MAX_SCALE = 1.0f;

        private final float MIN_ALPHA = 0.5f;
        private final float MAX_ALPHA = 1.0f;

        @Override
        public void prepare() {
            if (slidingLayersView.isOpen()) {
                backsideContainer.setScaleX(MAX_SCALE);
                backsideContainer.setScaleY(MAX_SCALE);
                backsideContainer.setAlpha(MAX_ALPHA);
            } else {
                backsideContainer.setScaleX(MIN_SCALE);
                backsideContainer.setScaleY(MIN_SCALE);
                backsideContainer.setAlpha(MIN_ALPHA);
            }
        }

        @Override
        public void frame(final float frameValue) {
            final float scale = Anime.interpolateFloats(frameValue, MIN_SCALE, MAX_SCALE);
            backsideContainer.setScaleX(scale);
            backsideContainer.setScaleY(scale);

            final float alpha = Anime.interpolateFloats(frameValue, MIN_ALPHA, MAX_ALPHA);
            backsideContainer.setAlpha(alpha);
        }

        @Override
        public void finish(final float finalFrameValue,
                           final long duration,
                           @NonNull final Interpolator interpolator,
                           @Nullable final AnimatorContext animatorContext) {
            final float finalScale = Anime.interpolateFloats(finalFrameValue, MIN_SCALE, MAX_SCALE);
            final float finalAlpha = Anime.interpolateFloats(finalFrameValue, MIN_ALPHA, MAX_ALPHA);
            animatorFor(backsideContainer, animatorContext)
                    .withDuration(duration)
                    .withInterpolator(interpolator)
                    .scale(finalScale)
                    .alpha(finalAlpha)
                    .start();
        }

        @Override
        public void cancel() {
            Anime.cancelAll(backsideContainer);

            backsideContainer.setScaleX(MAX_SCALE);
            backsideContainer.setScaleY(MAX_SCALE);
            backsideContainer.setAlpha(MAX_ALPHA);
        }
    }

    //endregion


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull final LocalDate startDate, @Nullable final Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_IN, null);

        final ZoomedOutTimelineFragment navigatorFragment =
                ZoomedOutTimelineFragment.newInstance(startDate, timeline);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_home_container, navigatorFragment, ZoomedOutTimelineFragment.TAG)
                .addToBackStack(ZoomedOutTimelineFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitAllowingStateLoss();
    }

    @Override
    public void onTimelineSelected(@NonNull final LocalDate date, @Nullable final Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        final int datePosition = viewPagerAdapter.getDatePosition(date);
        if (datePosition != viewPager.getCurrentItem()) {
            viewPagerAdapter.setCachedTimeline(timeline);
            viewPager.setCurrentItem(datePosition, false);
        } else {
            final TimelineFragment currentFragment =
                    (TimelineFragment) viewPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.scrollToTop();
            }
        }
        stateSafeExecutor.execute(getFragmentManager()::popBackStack);
    }

    //endregion
}
