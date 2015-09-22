package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import org.joda.time.LocalDate;
import org.json.JSONObject;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.timeline.PerspectiveTransformer;
import is.hello.sense.ui.widget.util.InteractiveAnimator;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.go99.Anime.isAnimating;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class HomeActivity extends ScopedInjectionActivity
        implements SlidingLayersView.Listener,
        ZoomedOutTimelineFragment.OnTimelineDateSelectedListener,
        AnimatorContext.Scene,
        ViewPager.OnPageChangeListener,
        TimelineInfoFragment.AnchorProvider {
    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    /**
     * Whether or not the <code>HomeActivity</code> was started in response
     * to the user finishing on-boarding.
     * <p>
     * Literal value is <code>is.hello.sense.ui.activities.HomeActivity.EXTRA_SHOW_UNDERSIDE</code>
     * and cannot be changed for backwards compatibility.
     */
    public static final String EXTRA_POST_ONBOARDING = HomeActivity.class.getName() + ".EXTRA_SHOW_UNDERSIDE";

    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Inject ApiService apiService;
    @Inject DeviceIssuesPresenter deviceIssuesPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;

    private long lastUpdated = Long.MAX_VALUE;

    private RelativeLayout rootContainer;
    private FrameLayout undersideContainer;
    private SlidingLayersView slidingLayersView;

    private ViewPager viewPager;
    private TimelineFragmentAdapter viewPagerAdapter;
    private int lastPagerScrollState = ViewPager.SCROLL_STATE_IDLE;
    private ImageButton smartAlarmButton;

    private boolean isFirstActivityRun;
    private boolean showUnderside;

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());


    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        deviceIssuesPresenter.bindScope(this);
        presenterContainer.addPresenter(deviceIssuesPresenter);

        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.showUnderside = false;
            this.lastUpdated = savedInstanceState.getLong("lastUpdated");
            presenterContainer.onRestoreState(savedInstanceState);
        } else {
            final boolean postOnboarding = isPostOnboarding();
            this.showUnderside = postOnboarding;

            if (NotificationRegistration.shouldRegister(this)) {
                new NotificationRegistration(this).register();
            }

            if (getIntent().hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
                dispatchNotification(getIntent().getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), false);
            }

            if (!postOnboarding) {
                updateUnreadInsightsState();
            }
        }

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(getIntent().getAction())) {
            JSONObject properties = Analytics.createProperties(
                    Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME, "ACTION_SHOW_ALARMS"
                                                              );
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            stateSafeExecutor.execute(() -> showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, false));
        }

        deviceIssuesPresenter.update();


        this.rootContainer = (RelativeLayout) findViewById(R.id.activity_home_container);


        this.smartAlarmButton = (ImageButton) findViewById(R.id.fragment_timeline_smart_alarm);
        Views.setSafeOnClickListener(smartAlarmButton, ignored -> {
            showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, true);
        });

        this.viewPager = (ViewPager) findViewById(R.id.activity_home_view_pager);
        viewPager.addOnPageChangeListener(this);
        if (BuildConfig.DEBUG) {
            viewPager.setPageTransformer(false, new PerspectiveTransformer());
        }

        this.viewPagerAdapter = new TimelineFragmentAdapter(getFragmentManager(),
                                                            preferences.getAccountCreationDate());
        viewPager.setAdapter(viewPagerAdapter);

        if (viewPager.getCurrentItem() == 0) {
            jumpToLastNight(false);
        }


        this.undersideContainer = (FrameLayout) findViewById(R.id.activity_home_underside_container);

        this.slidingLayersView = (SlidingLayersView) findViewById(R.id.activity_home_sliding_layers);
        slidingLayersView.setListener(this);
        slidingLayersView.setInteractiveAnimator(new UndersideAnimator());
        slidingLayersView.setAnimatorContext(getAnimatorContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            slidingLayersView.setBackgroundColor(getResources().getColor(R.color.status_bar));
        }


        Fragment navigatorFragment = getFragmentManager().findFragmentByTag(ZoomedOutTimelineFragment.TAG);
        if (navigatorFragment != null) {
            getFragmentManager().popBackStack(ZoomedOutTimelineFragment.TAG,
                                              FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            JSONObject properties = Analytics.createProperties(
                    Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME, "ACTION_SHOW_ALARMS"
                                                              );
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, false);
        } else if (intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), isResumed);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Observable<Intent> onLogOut = Rx.fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        bindAndSubscribe(onLogOut,
                         ignored -> {
                             startActivity(new Intent(this, OnboardingActivity.class));
                             finish();
                         },
                         Functions.LOG_ERROR);

        if (isFirstActivityRun && !isPostOnboarding()) {
            bindAndSubscribe(deviceIssuesPresenter.latest(),
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }

        checkInForUpdates();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("lastUpdated", lastUpdated);
        presenterContainer.onSaveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Distribution.checkForUpdates(this);

        if (showUnderside) {
            slidingLayersView.openWithoutAnimation();
            this.showUnderside = false;
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS) {
            if (isCurrentFragmentLastNight()) {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, reloading.");
                TimelineFragment fragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
                if (fragment != null) {
                    fragment.update();
                }

                this.lastUpdated = System.currentTimeMillis();
            } else {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
                viewPager.setCurrentItem(viewPagerAdapter.getLastNight(), false);
            }


            Fragment navigatorFragment = getFragmentManager().findFragmentByTag(ZoomedOutTimelineFragment.TAG);
            if (navigatorFragment != null) {
                getFragmentManager().popBackStack();
            }

            updateUnreadInsightsState();
        }

        presenterContainer.onContainerResumed();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        viewPager.removeOnPageChangeListener(this);

        if (isFinishing()) {
            presenterContainer.onContainerDestroyed();
        }
    }

    //endregion


    //region Notifications

    private void dispatchNotification(@NonNull Bundle notification, boolean animate) {
        stateSafeExecutor.execute(() -> {
            Logger.info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            Notification target = Notification.fromBundle(notification);
            switch (target) {
                case TIMELINE: {
                    if (slidingLayersView.isOpen()) {
                        slidingLayersView.close();
                    }

                    LocalDate date = Notification.getDate(notification);
                    int position = viewPagerAdapter.getDatePosition(date);
                    viewPager.setCurrentItem(position, false);

                    break;
                }
                case SENSOR: {
                    showUndersideWithItem(UndersideFragment.ITEM_ROOM_CONDITIONS, animate);

                    Intent sensorHistory = new Intent(this, SensorHistoryActivity.class);
                    String sensorName = Notification.getSensorName(notification);
                    sensorHistory.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorName);
                    startActivity(sensorHistory);

                    break;
                }
                case TRENDS: {
                    showUndersideWithItem(UndersideFragment.ITEM_TRENDS, animate);
                    break;
                }
                case ALARM: {
                    showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, animate);
                    break;
                }
                case SETTINGS: {
                    showUndersideWithItem(UndersideFragment.ITEM_APP_SETTINGS, animate);
                    break;
                }
                case INSIGHTS: {
                    showUndersideWithItem(UndersideFragment.ITEM_INSIGHTS, animate);
                    break;
                }
            }
        });
    }

    //endregion


    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            if (slidingLayersView.isOpen()) {
                if (!slidingLayersView.isInMotion()) {
                    UndersideFragment undersideFragment = getUndersideFragment();
                    if (undersideFragment == null || !undersideFragment.onBackPressed()) {
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


    public boolean isPostOnboarding() {
        return getIntent().getBooleanExtra(EXTRA_POST_ONBOARDING, false);
    }

    public boolean isUndersideVisible() {
        return (slidingLayersView.isOpen() ||
                slidingLayersView.isInMotion());
    }

    public boolean isCurrentFragmentLastNight() {
        TimelineFragment currentFragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }

    public void jumpToLastNight(boolean animate) {
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
                                 AppUpdateDialogFragment dialogFragment = AppUpdateDialogFragment.newInstance(response);
                                 dialogFragment.show(getFragmentManager(), AppUpdateDialogFragment.TAG);
                             }
                         },
                         e -> Logger.error(HomeActivity.class.getSimpleName(), "Could not run update check in", e));
    }

    public void updateUnreadInsightsState() {
        Logger.debug(getClass().getSimpleName(), "Updating unread insights state");
        apiService.unreadStats()
                  .subscribe(stats -> {
                                 final boolean hasUnreadInsightItems = (stats.hasUnreadInsights() &&
                                         stats.hasUnansweredQuestions());
                                 preferences.edit()
                                            .putBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS,
                                                        hasUnreadInsightItems)
                                            .apply();

                                 Logger.debug(getClass().getSimpleName(),
                                              "Updated unread insights state " + hasUnreadInsightItems);
                             },
                             Functions.LOG_ERROR);
    }


    //region Fragment Adapter

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        showAlarmShortcut();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (lastPagerScrollState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            animatorContext.beginAnimation("Timeline swipe");

            TimelineFragment currentFragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
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
    public View findAnimationAnchorView(@IdRes int viewId) {
        Fragment currentFragment = viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null && currentFragment.getView() != null) {
            return currentFragment.getView().findViewById(viewId);
        }

        return null;
    }

    public void hideAlarmShortcut() {
        if (smartAlarmButton.getVisibility() == View.VISIBLE && !isAnimating(smartAlarmButton)) {
            int contentHeight = rootContainer.getMeasuredHeight();

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

    public void bindDeviceIssue(@NonNull DeviceIssuesPresenter.Issue issue) {
        if (issue == DeviceIssuesPresenter.Issue.NONE ||
                getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null) {
            return;
        }

        localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);

        DeviceIssueDialogFragment deviceIssueDialogFragment = DeviceIssueDialogFragment.newInstance(issue);
        deviceIssueDialogFragment.showAllowingStateLoss(getFragmentManager(), DeviceIssueDialogFragment.TAG);
    }

    //endregion


    //region Sliding Layers

    private @Nullable UndersideFragment getUndersideFragment() {
        return (UndersideFragment) getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
    }

    @Override
    public void onTopViewWillSlideDown() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_OPENED, null);

        if (isResumed && getFragmentManager().findFragmentById(R.id.activity_home_underside_container) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_home_underside_container, new UndersideFragment())
                    .commit();
        }

        TimelineFragment currentFragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onTopViewWillSlideDown();
        }

        this.isFirstActivityRun = false;
    }

    @Override
    public void onTopViewDidSlideUp() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_CLOSED, null);

        UndersideFragment underside = getUndersideFragment();
        if (underside != null) {
            stateSafeExecutor.execute(() -> {
                getFragmentManager()
                        .beginTransaction()
                        .remove(underside)
                        .commit();
            });
        }

        TimelineFragment currentFragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onTopViewDidSlideUp();
        }
    }

    public void showUndersideWithItem(int item, boolean animate) {
        if (slidingLayersView.isOpen()) {
            UndersideFragment underside = getUndersideFragment();
            if (underside != null) {
                underside.setCurrentItem(item, UndersideFragment.OPTION_ANIMATE);
            }
        } else {
            UndersideFragment.saveCurrentItem(this, item);
            if (animate) {
                slidingLayersView.open();
            } else {
                slidingLayersView.openWithoutAnimation();
            }
        }
    }

    public void toggleUndersideVisible() {
        slidingLayersView.toggle();
    }

    private class UndersideAnimator implements InteractiveAnimator {
        private final float MIN_SCALE = 0.85f;
        private final float MAX_SCALE = 1.0f;

        private final float MIN_ALPHA = 0.5f;
        private final float MAX_ALPHA = 1.0f;

        @Override
        public void prepare() {
            if (slidingLayersView.isOpen()) {
                undersideContainer.setScaleX(MAX_SCALE);
                undersideContainer.setScaleY(MAX_SCALE);
                undersideContainer.setAlpha(MAX_ALPHA);
            } else {
                undersideContainer.setScaleX(MIN_SCALE);
                undersideContainer.setScaleY(MIN_SCALE);
                undersideContainer.setAlpha(MIN_ALPHA);
            }
        }

        @Override
        public void frame(float frameValue) {
            float scale = Anime.interpolateFloats(frameValue, MIN_SCALE, MAX_SCALE);
            undersideContainer.setScaleX(scale);
            undersideContainer.setScaleY(scale);

            float alpha = Anime.interpolateFloats(frameValue, MIN_ALPHA, MAX_ALPHA);
            undersideContainer.setAlpha(alpha);
        }

        @Override
        public void finish(float finalFrameValue,
                           long duration,
                           @NonNull Interpolator interpolator,
                           @Nullable AnimatorContext animatorContext) {
            float finalScale = Anime.interpolateFloats(finalFrameValue, MIN_SCALE, MAX_SCALE);
            float finalAlpha = Anime.interpolateFloats(finalFrameValue, MIN_ALPHA, MAX_ALPHA);
            animatorFor(undersideContainer, animatorContext)
                    .withDuration(duration)
                    .withInterpolator(interpolator)
                    .scale(finalScale)
                    .alpha(finalAlpha)
                    .start();
        }

        @Override
        public void cancel() {
            Anime.cancelAll(undersideContainer);

            undersideContainer.setScaleX(MAX_SCALE);
            undersideContainer.setScaleY(MAX_SCALE);
            undersideContainer.setAlpha(MAX_ALPHA);
        }
    }

    //endregion


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull LocalDate startDate, @Nullable Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_IN, null);

        ZoomedOutTimelineFragment navigatorFragment = ZoomedOutTimelineFragment.newInstance(startDate, timeline);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_home_container, navigatorFragment, ZoomedOutTimelineFragment.TAG)
                .addToBackStack(ZoomedOutTimelineFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commitAllowingStateLoss();
    }

    @Override
    public void onTimelineSelected(@NonNull LocalDate date, @Nullable Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        int datePosition = viewPagerAdapter.getDatePosition(date);
        if (datePosition != viewPager.getCurrentItem()) {
            viewPagerAdapter.setCachedTimeline(timeline);
            viewPager.setCurrentItem(datePosition, false);
        } else {
            TimelineFragment currentFragment = (TimelineFragment) viewPagerAdapter.getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.scrollToTop();
            }
        }
        stateSafeExecutor.execute(getFragmentManager()::popBackStack);
    }

    //endregion
}
