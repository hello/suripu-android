package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.seismic.ShakeDetector;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.InteractiveAnimator;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineNavigatorFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayFragment;
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
import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class HomeActivity
        extends InjectionActivity
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener, TimelineNavigatorFragment.OnTimelineDateSelectedListener, AnimatorContext.Scene
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
    private FragmentPageView<TimelineFragment> viewPager;
    private ImageButton smartAlarmButton;

    private @Nullable View deviceAlert;

    private boolean isFirstActivityRun;
    private boolean showUnderside;

    private @Nullable FragmentManager.OnBackStackChangedListener navigatorStackListener;

    private @Nullable SensorManager sensorManager;
    private @Nullable ShakeDetector shakeDetector;

    private final AnimatorContext animatorContext = new AnimatorContext(getClass().getSimpleName());

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
            coordinator.postOnResume(() -> showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, false));
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


        this.smartAlarmButton = (ImageButton) findViewById(R.id.fragment_timeline_smart_alarm);
        Views.setSafeOnClickListener(smartAlarmButton, ignored -> {
            showUndersideWithItem(UndersideFragment.ITEM_SMART_ALARM_LIST, true);
        });

        AnimatorContext animatorContext = getAnimatorContext();

        // noinspection unchecked
        this.viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getFragmentManager());
        viewPager.setAdapter(this);
        viewPager.setOnTransitionObserver(this);
        viewPager.setResumeCoordinator(coordinator);
        viewPager.setAnimatorContext(animatorContext);
        if (viewPager.getCurrentFragment() == null) {
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight(), null);
            viewPager.setCurrentFragment(fragment);
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
            bindAndSubscribe(devicesPresenter.devices.take(1),
                             this::bindDevices,
                             this::devicesUnavailable);
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
                underside.notifyTabSelected(false);
            });
            this.showUnderside = false;
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS && !isCurrentFragmentLastNight()) {
            Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight(), null);
            viewPager.setCurrentFragment(fragment);


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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (navigatorStackListener != null) {
            getFragmentManager().removeOnBackStackChangedListener(navigatorStackListener);
        }

        if (isFinishing()) {
            presenterContainer.onContainerDestroyed();
        }
    }

    //endregion


    //region Notifications

    private void dispatchNotification(@NonNull Bundle notification, boolean animate) {
        coordinator.postOnResume(() -> {
            Logger.info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            Notification target = Notification.fromBundle(notification);
            switch (target) {
                case TIMELINE: {
                    if (slidingLayersView.isOpen()) {
                        slidingLayersView.close();
                    }

                    DateTime date = Notification.getDate(notification);
                    TimelineFragment fragment = TimelineFragment.newInstance(date, null);
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
        if(slidingLayersView.isOpen()) {
            slidingLayersView.close();
        } else {
            super.onBackPressed();
        }
    }


    public boolean getWillShowUnderside() {
        return getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false);
    }

    public boolean isCurrentFragmentLastNight() {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
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
    public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return true;
    }

    @Override
    public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().minusDays(1), null);
    }


    @Override
    public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        DateTime fragmentTime = fragment.getDate();
        return fragmentTime.isBefore(DateFormatter.lastNight().withTimeAtStartOfDay());
    }

    @Override
    public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().plusDays(1), null);
    }


    @Override
    public void onWillTransitionToFragment(@NonNull TimelineFragment fragment, boolean isInteractive) {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.setControlsAlarmShortcut(false);
        }

        showAlarmShortcut();
    }

    @Override
    public void onDidTransitionToFragment(@NonNull TimelineFragment fragment, boolean isInteractive) {
        this.lastUpdated = System.currentTimeMillis();

        fragment.setControlsAlarmShortcut(true);

        if (isInteractive) {
            TutorialOverlayFragment.markShown(this, Tutorial.SWIPE_TIMELINE);
        }
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_DATE_CHANGED, null);
    }

    @Override
    public void onDidSnapBackToFragment(@NonNull TimelineFragment fragment) {
        fragment.setControlsAlarmShortcut(true);
    }

    //endregion


    //region Smart Alarm Button

    public void hideAlarmShortcut() {
        if (smartAlarmButton.getVisibility() == View.VISIBLE && !isAnimating(smartAlarmButton)) {
            int contentHeight = rootContainer.getMeasuredHeight();

            animate(smartAlarmButton, animatorContext)
                    .y(contentHeight)
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
            int contentHeight = rootContainer.getMeasuredHeight();
            int buttonHeight = smartAlarmButton.getMeasuredHeight();

            smartAlarmButton.setVisibility(View.VISIBLE);

            animate(smartAlarmButton, animatorContext)
                    .y(contentHeight - buttonHeight)
                    .start();
        }
    }

    //endregion


    //region Alerts

    public void showDevices() {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(R.string.label_devices), DeviceListFragment.class, null);
        Intent intent = new Intent(this, FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    public void bindDevices(@NonNull ArrayList<Device> devices) {
        Map<Device.Type, Device> devicesMap = Device.getDevicesMap(devices);
        Device sense = devicesMap.get(Device.Type.SENSE);
        Device pill = devicesMap.get(Device.Type.PILL);

        if (sense != null) {
            Analytics.setSenseId(sense.getDeviceId());
        }

        if (sense == null) {
            showDeviceAlert(R.string.alert_title_no_sense, R.string.alert_message_no_sense, this::showDevices);
        } else if (sense.getHoursSinceLastUpdated() >= Device.MISSING_THRESHOLD_HRS) {
            showDeviceAlert(R.string.alert_title_missing_sense, R.string.alert_message_missing_sense, this::showDevices);
        } else if (pill == null) {
            showDeviceAlert(R.string.alert_title_no_pill, R.string.alert_message_no_pill, this::showDevices);
        } else if (pill.getHoursSinceLastUpdated() >= Device.MISSING_THRESHOLD_HRS) {
            showDeviceAlert(R.string.alert_title_missing_pill, R.string.alert_message_missing_pill, this::showDevices);
        } else {
            hideDeviceAlert();
        }
    }

    public void devicesUnavailable(Throwable e) {
        Logger.error(getClass().getSimpleName(), "Devices list was unavailable.", e);
        hideDeviceAlert();
    }

    public void showDeviceAlert(@StringRes int titleRes,
                                @StringRes int messageRes,
                                @NonNull Runnable action) {
        if (deviceAlert != null) {
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        this.deviceAlert = inflater.inflate(R.layout.item_bottom_alert, rootContainer, false);

        TextView title = (TextView) deviceAlert.findViewById(R.id.item_bottom_alert_title);
        title.setText(titleRes);

        TextView message = (TextView) deviceAlert.findViewById(R.id.item_bottom_alert_message);
        message.setText(messageRes);

        Button later = (Button) deviceAlert.findViewById(R.id.item_bottom_alert_later);
        Views.setSafeOnClickListener(later, ignored -> hideDeviceAlert());

        Button fixNow = (Button) deviceAlert.findViewById(R.id.item_bottom_alert_fix_now);
        Views.setSafeOnClickListener(fixNow, ignored -> {
            hideDeviceAlert();
            action.run();
        });

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) deviceAlert.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        deviceAlert.setVisibility(View.INVISIBLE);

        Views.observeNextLayout(deviceAlert).subscribe(container -> {
            int alertViewHeight = deviceAlert.getMeasuredHeight();
            int alertViewY = (int) deviceAlert.getY();

            deviceAlert.setY(alertViewY + alertViewHeight);
            deviceAlert.setVisibility(View.VISIBLE);

            animate(deviceAlert, animatorContext)
                    .y(alertViewY)
                    .start();
        });
        rootContainer.post(() -> rootContainer.addView(deviceAlert));
    }

    public void hideDeviceAlert() {
        if (deviceAlert == null) {
            return;
        }

        coordinator.postOnResume(() -> {
            int alertViewHeight = deviceAlert.getMeasuredHeight();
            int alertViewY = (int) deviceAlert.getY();
            animate(deviceAlert, animatorContext)
                    .y(alertViewY + alertViewHeight)
                    .addOnAnimationCompleted(finished -> {
                        rootContainer.removeView(deviceAlert);
                        this.deviceAlert = null;
                    })
                    .start();
        });
    }

    //endregion


    //region Sliding Layers

    public SlidingLayersView getSlidingLayersView() {
        return slidingLayersView;
    }

    private UndersideFragment getUndersideFragment() {
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

        viewPager.getCurrentFragment().onUserWillPullDownTopView();

        this.isFirstActivityRun = false;
    }

    @Override
    public void onUserDidPushUpTopView() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_CLOSED, null);

        UndersideFragment underside = getUndersideFragment();
        if (underside != null) {
            coordinator.postOnResume(() -> {
                getFragmentManager()
                        .beginTransaction()
                        .remove(underside)
                        .commit();
            });
        }

        viewPager.getCurrentFragment().onUserDidPushUpTopView();
    }

    public void showUndersideWithItem(int item, boolean animate) {
        if (slidingLayersView.isOpen()) {
            UndersideFragment underside = getUndersideFragment();
            underside.setCurrentItem(item, UndersideFragment.OPTION_ANIMATE | UndersideFragment.OPTION_NOTIFY);
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
                    .addOnAnimationCompleted(finished -> {
                        if (!finished)
                            return;

                        if (slidingLayersView.isOpen()) {
                            UndersideFragment underside = getUndersideFragment();
                            underside.notifyTabSelected(false);
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

        ViewGroup undersideContainer = (ViewGroup) findViewById(R.id.activity_home_content_container);

        TimelineNavigatorFragment navigatorFragment = TimelineNavigatorFragment.newInstance(startDate, timeline);
        navigatorFragment.show(getFragmentManager(), 0, TimelineNavigatorFragment.TAG);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();

        View view = navigatorFragment.getView();
        if (view == null) {
            throw new IllegalStateException();
        }

        this.navigatorStackListener = () -> {
            if (!navigatorFragment.isAdded() && !isDestroyed()) {
                showAlarmShortcut();
                animate(viewPager, animatorContext)
                        .zoomInFrom(0.7f)
                        .addOnAnimationCompleted(finished -> {
                            if (finished) {
                                undersideContainer.removeView(view);
                                slidingLayersView.setEnabled(true);
                            }
                        })
                        .start();

                fragmentManager.removeOnBackStackChangedListener(navigatorStackListener);
                this.navigatorStackListener = null;
            }
        };
        fragmentManager.addOnBackStackChangedListener(navigatorStackListener);

        slidingLayersView.setEnabled(false);
        undersideContainer.addView(view, 0);

        hideAlarmShortcut();
        animate(viewPager, animatorContext)
                .zoomOutTo(View.GONE, 0.7f)
                .start();
    }

    @Override
    public void onTimelineSelected(@NonNull DateTime date, @Nullable Timeline timeline) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_ZOOMED_OUT, null);

        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        if (!date.equals(currentFragment.getDate())) {
            viewPager.setCurrentFragment(TimelineFragment.newInstance(date, timeline));
        } else {
            currentFragment.scrollToTop();
        }
        getFragmentManager().popBackStack();
    }

    //endregion
}
