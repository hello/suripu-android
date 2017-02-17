package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.ui.fragments.FeedPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsPresenterFragment;
import is.hello.sense.flows.home.ui.views.SenseTabLayout;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationInteractor;
import is.hello.sense.notifications.NotificationMessageReceiver;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.SystemAlertDialogFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

import static android.provider.AlarmClock.ACTION_SHOW_ALARMS;
import static is.hello.sense.util.Logger.info;


public class HomeActivity extends ScopedInjectionActivity
        implements
        Alert.ActionHandler,
        InsightInfoFragment.ParentProvider,
        TimelineFragment.ParentProvider,
        OnboardingFlowProvider,
        SenseTabLayout.Listener {

    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    private static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";
    private static final String KEY_CURRENT_ITEM_INDEX = HomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static boolean isFirstActivityRun = true; // changed when paused

    @Inject
    ApiService apiService;
    @Inject
    AlertsInteractor alertsInteractor;
    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;
    @Inject
    LastNightInteractor lastNightInteractor;
    @Inject
    UnreadStateInteractor unreadStateInteractor;
    @Inject
    NotificationInteractor notificationInteractor;

    private final HomeViewPagerDelegate viewPagerDelegate = new HomeViewPagerDelegate();
    private View progressOverlay;
    private SpinnerImageView spinner;
    private ExtendedViewPager extendedViewPager;
    private SenseTabLayout tabLayout;
    //todo revert to true after testing
    private final BroadcastReceiver notificationReceiver = new NotificationMessageReceiver(false);

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
        this.deviceIssuesPresenter.bindScope(this);
        addInteractor(this.deviceIssuesPresenter);
        addInteractor(this.alertsInteractor);
        addInteractor(this.lastNightInteractor);
        addInteractor(this.unreadStateInteractor);
        setContentView(R.layout.activity_new_home);
        this.progressOverlay = findViewById(R.id.activity_new_home_progress_overlay);
        this.spinner = (SpinnerImageView) this.progressOverlay.findViewById(R.id.activity_new_home_spinner);
        this.extendedViewPager = (ExtendedViewPager) findViewById(R.id.activity_new_home_extended_view_pager);
        this.extendedViewPager.setScrollingEnabled(false);
        this.extendedViewPager.setFadePageTransformer(true);
        this.extendedViewPager.setOffscreenPageLimit(this.viewPagerDelegate.getOffscreenPageLimit());
        this.tabLayout = (SenseTabLayout) findViewById(R.id.activity_new_home_tab_layout);
        restoreState(savedInstanceState);
        this.tabLayout.setupWithViewPager(this.extendedViewPager);
        final StaticFragmentAdapter fragmentAdapter = new StaticFragmentAdapter(getFragmentManager(),
                                                                                this.extendedViewPager.getId(),
                                                                                this.viewPagerDelegate.getViewPagerItems());
        this.extendedViewPager.setAdapter(fragmentAdapter);
        this.tabLayout.setUpTabs(savedInstanceState == null);
        this.tabLayout.setListener(this);
        //todo needs testing with server
        final Intent intent = getIntent();
        if(intent != null && intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD));
        }

        registerReceiver(
                notificationReceiver,
                NotificationMessageReceiver.getMainPriorityFilter());
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final IntentFilter loggedOutIntent = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(getApplicationContext(), loggedOutIntent);
        bindAndSubscribe(onLogOut,
                         ignored -> finish(),
                         Functions.LOG_ERROR);
        if (shouldUpdateDeviceIssues()) {
            bindAndSubscribe(this.deviceIssuesPresenter.topIssue,
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }
        bindAndSubscribe(this.alertsInteractor.alert,
                         this::bindAlert,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.lastNightInteractor.timeline,
                         this.tabLayout::updateSleepScoreTab,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.unreadStateInteractor.hasUnreadItems,
                         this::bindUnreadItems,
                         Functions.LOG_ERROR);
        this.lastNightInteractor.update();
        this.unreadStateInteractor.update();
        checkInForUpdates();
    }

    @Override
    public void onBackPressed() {
        if (this.progressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState, final PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, this.tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.lastNightInteractor.update();
        if (shouldUpdateAlerts()) {
            this.alertsInteractor.update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFirstActivityRun = false;
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SHOW_ALARMS.equals(intent.getAction())) {
            final Properties properties =
                    Analytics.createProperties(Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME,
                                               "ACTION_SHOW_ALARMS");
            Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
            this.tabLayout.selectSoundTab();
        } else if (intent.hasExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.tabLayout != null) {
            this.tabLayout.clearOnTabSelectedListeners();
        }

        unregisterReceiver(notificationReceiver);
    }

    public void checkInForUpdates() {
        bindAndSubscribe(this.apiService.checkInForUpdates(new UpdateCheckIn()),
                         response -> {
                             if (response.isNewVersion()) {
                                 final AppUpdateDialogFragment dialogFragment =
                                         AppUpdateDialogFragment.newInstance(response);
                                 dialogFragment.show(getFragmentManager(), AppUpdateDialogFragment.TAG);
                             }
                         },
                         e -> Logger.error(HomeActivity.class.getSimpleName(), "Could not run update check in", e));
    }


    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.tabLayout.setCurrentItemIndex(savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX,
                                                                         this.viewPagerDelegate.getStartingItemPosition()));
        } else {
            this.tabLayout.setCurrentItemIndex(this.viewPagerDelegate.getStartingItemPosition());
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

    //end region

    //region Device Issues and Alerts

    private boolean isShowingAlert() {
        return getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) != null
                || getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null;
    }

    private boolean shouldUpdateAlerts() {
        return getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
    }

    private boolean shouldUpdateDeviceIssues() {
        return isFirstActivityRun && getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
    }

    private boolean shouldShow(@NonNull final Alert alert) {
        final boolean valid = alert.isValid();
        final boolean existingAlert = this.isShowingAlert();
        switch (alert.getCategory()) {
            case EXPANSION_UNREACHABLE:
                return valid && !existingAlert; // always show valid unreacahable alerts whenever we get them
            case SENSE_MUTED:
            default:
                return valid && !existingAlert && isFirstActivityRun;
        }
    }

    private void bindUnreadItems(final boolean hasUnreadItems) {
        tabLayout.setHomeTabIndicatorVisible(hasUnreadItems);
    }

    private void bindAlert(@NonNull final Alert alert) {
        if (shouldShow(alert)) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
            SystemAlertDialogFragment.newInstance(alert,
                                                  getResources())
                                     .showAllowingStateLoss(getFragmentManager(),
                                                            R.id.activity_new_home_bottom_alert_container,
                                                            BottomAlertDialogFragment.TAG);
        } else if (shouldUpdateDeviceIssues()) {
            this.deviceIssuesPresenter.update();
        }
    }

    private void bindDeviceIssue(@NonNull final DeviceIssuesInteractor.Issue issue) {
        if (issue == DeviceIssuesInteractor.Issue.NONE || this.isShowingAlert()) {
            return;
        }

        localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
        DeviceIssueDialogFragment.newInstance(issue,
                                              getResources())
                                 .showAllowingStateLoss(getFragmentManager(),
                                                        R.id.activity_new_home_bottom_alert_container,
                                                        DeviceIssueDialogFragment.TAG);

        this.deviceIssuesPresenter.updateLastShown(issue);
    }

    public void jumpToLastNight() {
        final TimelineFragment.Parent parent = getTimelineParent();
        if (parent != null) {
            parent.jumpToLastNight();
        }
    }
    //endregion

    //region Alert Action Handler

    @Override
    public void unMuteSense() {
        showProgressOverlay(true);
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
             );
    }

    //endregion

    public void showProgressOverlay(final boolean show) {
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
    }

    @Override
    public TimelineFragment.Parent getTimelineParent() {
        return (TimelineFragment.Parent) getFragmentWithIndex(SenseTabLayout.SLEEP_ICON_KEY);
    }

    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment parentProvider = getFragmentWithIndex(SenseTabLayout.INSIGHTS_ICON_KEY);
        if (parentProvider instanceof InsightInfoFragment.ParentProvider) {
            return ((InsightInfoFragment.ParentProvider) parentProvider).provideInsightInfoParent();
        } else {
            return null;
        }
    }

    @Nullable
    private Fragment getFragmentWithIndex(final int index) {
        if (extendedViewPager != null && extendedViewPager.getAdapter() instanceof StaticFragmentAdapter) {
            return ((StaticFragmentAdapter) extendedViewPager.getAdapter()).getFragment(index);
        }
        return null;
    }

    //region Notifications

    private void dispatchNotification(@NonNull final Bundle notification) {
        this.stateSafeExecutor.execute(() -> {
            info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            @Notification.Type
            final String target = Notification.typeFromBundle(notification);
            switch (target) {
                case Notification.SLEEP_SCORE: {
                    this.tabLayout.selectTimelineTab();
                    //todo consider serializing notification in bundle
                    notificationInteractor.notificationSubject.onNext(new Notification(target, "2017-02-10"));

                    break;
                }
                case Notification.SYSTEM: {
                    //todo handle and pass along
                    this.tabLayout.selectConditionsTab();
                    break;
                }
                default:{
                    info(getClass().getSimpleName(), "unsupported notification type " + target);
                }
            }
        });
    }

    //endregion

    //region SenseTabLayout.Listener
    @Override
    public void scrollUp(final int fragmentPosition) {
        final Fragment fragment = getFragmentWithIndex(fragmentPosition);
        if (fragment instanceof HomeActivity.ScrollUp) {
            ((HomeActivity.ScrollUp) fragment).scrollUp();
        }
    }

    @Override
    public void tabChanged(final int fragmentPosition) {
        if (!this.lastNightInteractor.timeline.hasValue()) {
            this.lastNightInteractor.update();
        }
        this.unreadStateInteractor.update();
        this.extendedViewPager.setCurrentItem(fragmentPosition);
        if (fragmentPosition == SenseTabLayout.SLEEP_ICON_KEY) {
            jumpToLastNight();
        }
    }

    @Nullable
    @Override
    public Timeline getCurrentTimeline() {
        if (this.lastNightInteractor.timeline.hasValue()) {
            return this.lastNightInteractor.timeline.getValue();
        }
        return null;
    }
    //endregion


    public interface ScrollUp {
        void scrollUp();
    }

    private static class HomeViewPagerDelegate extends BaseViewPagerPresenterDelegate {

        @NonNull
        @Override
        public StaticFragmentAdapter.Item[] getViewPagerItems() {
            return new StaticFragmentAdapter.Item[]{
                    new StaticFragmentAdapter.Item(TimelinePagerPresenterFragment.class, TimelinePagerPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(TrendsPresenterFragment.class, TrendsPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(FeedPresenterFragment.class, FeedPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(SoundsPresenterFragment.class, SoundsPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(RoomConditionsPresenterFragment.class, RoomConditionsPresenterFragment.class.getSimpleName())
            };
        }

        @Override
        public int getOffscreenPageLimit() {
            return 4;
        }
    }


}