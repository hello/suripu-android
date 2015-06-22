package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.squareup.seismic.ShakeDetector;

import org.joda.time.DateTime;
import org.json.JSONObject;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.ui.adapter.TimelineFragmentAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.InteractiveAnimator;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineNavigatorFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.widget.FragmentPageTitleStrip;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Logger;
import is.hello.sense.util.RateLimitingShakeListener;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.isAnimating;
import static rx.android.content.ContentObservable.fromLocalBroadcast;

public class HomeActivity
        extends ScopedInjectionActivity
        implements FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener, TimelineNavigatorFragment.OnTimelineDateSelectedListener, AnimatorContext.Scene
{
    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    public static final String EXTRA_SHOW_UNDERSIDE = HomeActivity.class.getName() + ".EXTRA_SHOW_UNDERSIDE";

    private final PresenterContainer presenterContainer = new PresenterContainer();

    @Inject ApiService apiService;
    @Inject DevicesPresenter devicesPresenter;

    private long lastUpdated = Long.MAX_VALUE;

    private RelativeLayout rootContainer;
    private FrameLayout undersideContainer;
    private SlidingLayersView slidingLayersView;

    private ImageButton overflowButton;
    private ImageButton shareButton;
    private FragmentPageTitleStrip pagerTitleStrip;
    private FragmentPageView<TimelineFragment> viewPager;
    private ImageButton smartAlarmButton;

    private boolean isFirstActivityRun;
    private boolean showUnderside;

    private @Nullable SensorManager sensorManager;
    private @Nullable ShakeDetector shakeDetector;

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());
    private TimelineFragmentAdapter viewPagerAdapter;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        presenterContainer.addPresenter(devicesPresenter);

        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.showUnderside = false;
            this.lastUpdated = savedInstanceState.getLong("lastUpdated");
            presenterContainer.onRestoreState(savedInstanceState);
        } else {
            this.showUnderside = getWillShowUnderside();

            if (NotificationRegistration.shouldRegister(this)) {
                new NotificationRegistration(this).register();
            }

            if (getIntent().hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
                dispatchNotification(getIntent().getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), false);
            }
        }

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(getIntent().getAction())) {
            stateSafeExecutor.execute(() -> showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, false));
        }

        devicesPresenter.update();


        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            this.shakeDetector = new ShakeDetector(new RateLimitingShakeListener(() -> {
                Intent intent = new Intent(this, DebugActivity.class);
                startActivity(intent);
            }));
        }


        this.rootContainer = (RelativeLayout) findViewById(R.id.activity_home_container);


        this.overflowButton = (ImageButton) findViewById(R.id.activity_home_timeline_header_overflow);
        Views.setSafeOnClickListener(overflowButton, ignored -> slidingLayersView.toggle());

        this.shareButton = (ImageButton) findViewById(R.id.activity_home_timeline_header_share);
        shareButton.setVisibility(View.INVISIBLE);
        Views.setSafeOnClickListener(shareButton, ignored -> {
            TimelineFragment currentTimeline = viewPager.getCurrentFragment();
            if (currentTimeline == null) {
                return;
            }

            currentTimeline.share();
        });

        this.pagerTitleStrip = (FragmentPageTitleStrip) findViewById(R.id.activity_home_timeline_date);
        Views.setSafeOnClickListener(pagerTitleStrip, ignored -> {
            TimelineFragment currentTimeline = viewPager.getCurrentFragment();
            if (currentTimeline == null) {
                return;
            }

            showTimelineNavigator(currentTimeline.getDate(), currentTimeline.getCachedTimeline());
        });

        this.smartAlarmButton = (ImageButton) findViewById(R.id.fragment_timeline_smart_alarm);
        Views.setSafeOnClickListener(smartAlarmButton, ignored -> {
            showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, true);
        });

        AnimatorContext animatorContext = getAnimatorContext();

        // noinspection unchecked
        this.viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);

        this.viewPagerAdapter = new TimelineFragmentAdapter(getResources());
        viewPager.setAdapter(viewPagerAdapter);

        viewPager.setFragmentManager(getFragmentManager());
        viewPager.setOnTransitionObserver(this);
        viewPager.setDecor(pagerTitleStrip);
        viewPager.setStateSafeExecutor(stateSafeExecutor);
        viewPager.setAnimatorContext(animatorContext);

        if (viewPager.getCurrentFragment() == null) {
            jumpToLastNight(false);
        }


        this.undersideContainer = (FrameLayout) findViewById(R.id.activity_home_underside_container);

        this.slidingLayersView = (SlidingLayersView) findViewById(R.id.activity_home_sliding_layers);
        slidingLayersView.setOnInteractionListener(this);
        slidingLayersView.setInteractiveAnimator(new UndersideAnimator());
        slidingLayersView.setGestureInterceptingChild(viewPager);
        slidingLayersView.setAnimatorContext(animatorContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            slidingLayersView.setBackgroundColor(getResources().getColor(R.color.status_bar));
        }


        Fragment navigatorFragment = getFragmentManager().findFragmentByTag(TimelineNavigatorFragment.TAG);
        if (navigatorFragment != null) {
            getFragmentManager().popBackStack(TimelineNavigatorFragment.TAG,
                                              FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (AlarmClock.ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, false);
        } else if (intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD), isResumed);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Observable<Intent> onLogOut = fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        bindAndSubscribe(onLogOut,
                         ignored -> {
                             startActivity(new Intent(this, OnboardingActivity.class));
                             finish();
                         },
                         Functions.LOG_ERROR);

        if (isFirstActivityRun && !getWillShowUnderside()) {
            bindAndSubscribe(devicesPresenter.latestTopIssue(),
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }

        UndersideFragment underside = getUndersideFragment();
        if (underside != null) {
            underside.notifyTabSelected(false);
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

        if (shakeDetector != null && sensorManager != null) {
            shakeDetector.start(sensorManager);
        }

        Distribution.checkForUpdates(this);

        if (showUnderside) {
            slidingLayersView.openWithoutAnimation();
            slidingLayersView.post(() -> {
                UndersideFragment underside = getUndersideFragment();
                if (underside != null) {
                    underside.notifyTabSelected(false);
                }
            });
            this.showUnderside = false;
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS) {
            if (isCurrentFragmentLastNight()) {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, reloading.");
                TimelineFragment fragment = viewPager.getCurrentFragment();
                fragment.update();

                this.lastUpdated = System.currentTimeMillis();
            } else {
                Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
                TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight(), null, true);
                viewPager.setCurrentFragment(fragment);
            }


            Fragment navigatorFragment = getFragmentManager().findFragmentByTag(TimelineNavigatorFragment.TAG);
            if (navigatorFragment != null) {
                getFragmentManager().popBackStack();
            }
        }

        presenterContainer.onContainerResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (shakeDetector != null) {
            shakeDetector.stop();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            viewPagerAdapter.clearPlaceholder();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

                    DateTime date = Notification.getDate(notification);
                    TimelineFragment fragment = TimelineFragment.newInstance(date, null, false);
                    viewPager.setCurrentFragment(fragment);

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
                if (!slidingLayersView.isAnimating() && !slidingLayersView.hasActiveGesture()) {
                    UndersideFragment undersideFragment = getUndersideFragment();
                    if (undersideFragment == null || !undersideFragment.onBackPressed()) {
                        slidingLayersView.close();
                    }
                }

                return;
            } else if (!isCurrentFragmentLastNight()) {
                if (!viewPager.isAnimating() && !viewPager.hasActiveGesture()) {
                    jumpToLastNight(true);
                }

                return;
            }
        }

        super.onBackPressed();
    }


    public boolean getWillShowUnderside() {
        return getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false);
    }

    public boolean isUndersideVisible() {
        return slidingLayersView.isOpen();
    }

    public boolean isCurrentFragmentLastNight() {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }

    public void jumpToLastNight(boolean animate) {
        TimelineFragment lastNight = TimelineFragment.newInstance(DateFormatter.lastNight(), null, !animate);
        if (animate) {
            viewPager.animateToFragment(lastNight, FragmentPageView.Position.AFTER);
        } else {
            viewPager.setCurrentFragment(lastNight);
        }
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


    //region Fragment Adapter

    @Override
    public void onWillTransitionToFragment(@NonNull TimelineFragment fragment, boolean isInteractive) {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.setControlsSharedChrome(false);
        }
        setShareButtonVisible(false);

        showAlarmShortcut();
    }

    @Override
    public void onDidTransitionToFragment(@NonNull TimelineFragment fragment, boolean isInteractive) {
        this.lastUpdated = System.currentTimeMillis();

        fragment.setControlsSharedChrome(true);

        setShareButtonVisible(fragment.getWantsShareButton());

        if (isInteractive) {
            Tutorial.SWIPE_TIMELINE.markShown(this);

            JSONObject properties = Analytics.createProperties(
                    Analytics.Timeline.PROP_DATE, fragment.getDate().toString()
            );
            Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_SWIPE, properties);
        }
    }

    @Override
    public void onDidSnapBackToFragment(@NonNull TimelineFragment fragment) {
        setShareButtonVisible(fragment.getWantsShareButton());
        fragment.setControlsSharedChrome(true);
    }

    //endregion


    //region Shared Chrome

    public void hideAlarmShortcut() {
        if (smartAlarmButton.getVisibility() == View.VISIBLE && !isAnimating(smartAlarmButton)) {
            int contentHeight = rootContainer.getMeasuredHeight();

            animate(smartAlarmButton, animatorContext)
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

            animate(smartAlarmButton, animatorContext)
                    .translationY(0f)
                    .start();
        }
    }

    public void setShareButtonVisible(boolean visible) {
        if (visible && !slidingLayersView.isOpen()) {
            shareButton.setVisibility(View.VISIBLE);
        } else {
            shareButton.setVisibility(View.INVISIBLE);
        }
    }

    //endregion


    //region Device Issues

    public void bindDeviceIssue(@NonNull DevicesPresenter.Issue issue) {
        if (issue == DevicesPresenter.Issue.NONE ||
                getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null) {
            return;
        }

        DeviceIssueDialogFragment deviceIssueDialogFragment = DeviceIssueDialogFragment.newInstance(issue);
        deviceIssueDialogFragment.show(getFragmentManager(), DeviceIssueDialogFragment.TAG);
    }

    //endregion


    //region Sliding Layers

    private @Nullable UndersideFragment getUndersideFragment() {
        return (UndersideFragment) getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
    }

    @Override
    public void onUserWillPullDownTopView() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_OPENED, null);

        if (isResumed && getFragmentManager().findFragmentById(R.id.activity_home_underside_container) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_home_underside_container, new UndersideFragment())
                    .commit();
        }

        setShareButtonVisible(false);
        overflowButton.setImageResource(R.drawable.icon_menu_open);
        pagerTitleStrip.setDimmed(true);

        this.isFirstActivityRun = false;
    }

    @Override
    public void onUserDidPushUpTopView() {
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

        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        setShareButtonVisible(currentFragment != null && currentFragment.getWantsShareButton());
        overflowButton.setImageResource(R.drawable.icon_menu_closed);
        pagerTitleStrip.setDimmed(false);
    }

    public void showUndersideWithItem(int item, boolean animate) {
        if (slidingLayersView.isOpen()) {
            UndersideFragment underside = getUndersideFragment();
            if (underside != null) {
                underside.setCurrentItem(item, UndersideFragment.OPTION_ANIMATE | UndersideFragment.OPTION_NOTIFY);
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
            float scale = Animation.interpolateFrame(frameValue, MIN_SCALE, MAX_SCALE);
            undersideContainer.setScaleX(scale);
            undersideContainer.setScaleY(scale);

            float alpha = Animation.interpolateFrame(frameValue, MIN_ALPHA, MAX_ALPHA);
            undersideContainer.setAlpha(alpha);
        }

        @Override
        public void finish(float finalFrameValue,
                           long duration,
                           @NonNull Interpolator interpolator,
                           @Nullable AnimatorContext animatorContext) {
            float finalScale = Animation.interpolateFrame(finalFrameValue, MIN_SCALE, MAX_SCALE);
            float finalAlpha = Animation.interpolateFrame(finalFrameValue, MIN_ALPHA, MAX_ALPHA);
            animate(undersideContainer, animatorContext)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .scale(finalScale)
                    .alpha(finalAlpha)
                    .bindListeners(HomeActivity.this)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished)
                            return;

                        if (slidingLayersView.isOpen()) {
                            UndersideFragment underside = getUndersideFragment();
                            if (underside != null) {
                                underside.notifyTabSelected(false);
                            }
                        }
                    })
                    .start();
        }

        @Override
        public void cancel() {
            PropertyAnimatorProxy.stop(undersideContainer);

            undersideContainer.setScaleX(MAX_SCALE);
            undersideContainer.setScaleY(MAX_SCALE);
            undersideContainer.setAlpha(MAX_ALPHA);
        }
    }

    //endregion


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull DateTime startDate, @Nullable Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_IN, null);

        TimelineNavigatorFragment navigatorFragment = TimelineNavigatorFragment.newInstance(startDate, timeline);
        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_home_container, navigatorFragment, TimelineNavigatorFragment.TAG)
                .addToBackStack(TimelineNavigatorFragment.TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void onTimelineSelected(@NonNull DateTime date, @Nullable Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        if (!date.equals(currentFragment.getDate())) {
            viewPager.setCurrentFragment(TimelineFragment.newInstance(date, timeline, false));
        } else {
            currentFragment.scrollToTop();
        }
        getFragmentManager().popBackStack();
    }

    //endregion
}
