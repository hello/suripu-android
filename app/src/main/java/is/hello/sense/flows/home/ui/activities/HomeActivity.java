package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
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
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerFragment;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.mvp.presenters.HomePresenterFragment;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.mvp.presenters.TrendsPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.FabPresenter;
import is.hello.sense.mvp.util.FabPresenterProvider;
import is.hello.sense.notifications.Notification;
import is.hello.sense.notifications.NotificationType;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.adapter.FragmentPagerAdapter;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.SystemAlertDialogFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;
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
        FabPresenterProvider,
        OnboardingFlowProvider {

    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    private static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";
    private static final String KEY_CURRENT_ITEM_INDEX = HomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";

    private static final int NUMBER_OF_ITEMS = 5;
    private static final int SLEEP_ICON_KEY = 0;
    private static final int TRENDS_ICON_KEY = 1;
    private static final int INSIGHTS_ICON_KEY = 2;
    private static final int SOUNDS_ICON_KEY = 3;
    private static final int CONDITIONS_ICON_KEY = 4;

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

    private final Drawable[] drawables = new Drawable[NUMBER_OF_ITEMS];
    private final Drawable[] drawablesActive = new Drawable[NUMBER_OF_ITEMS];
    private final HomeViewPagerDelegate viewPagerDelegate = new HomeViewPagerDelegate();
    private int currentItemIndex;
    private View progressOverlay;
    private SpinnerImageView spinner;
    private ExtendedViewPager extendedViewPager;
    private TabLayout tabLayout;

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

        setContentView(R.layout.activity_new_home);
        restoreState(savedInstanceState);
        this.progressOverlay = findViewById(R.id.activity_new_home_progress_overlay);
        this.spinner = (SpinnerImageView) progressOverlay.findViewById(R.id.activity_new_home_spinner);
        this.extendedViewPager = (ExtendedViewPager) findViewById(R.id.activity_new_home_extended_view_pager);
        this.extendedViewPager.setScrollingEnabled(false);
        this.extendedViewPager.setFadePageTransformer(true);
        this.extendedViewPager.setOffscreenPageLimit(viewPagerDelegate.getOffscreenPageLimit());
        this.tabLayout = (TabLayout) findViewById(R.id.activity_new_home_tab_layout);
        this.tabLayout.setupWithViewPager(this.extendedViewPager);
        extendedViewPager.setAdapter(new StaticFragmentAdapter(getFragmentManager(),
                                                               viewPagerDelegate.getViewPagerItems()));
        setUpTabs(savedInstanceState == null);
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
            bindAndSubscribe(deviceIssuesPresenter.topIssue,
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }

        bindAndSubscribe(alertsInteractor.alert,
                         this::bindAlert,
                         Functions.LOG_ERROR);
        bindAndSubscribe(lastNightInteractor.timeline,
                         this::updateSleepScoreTab,
                         Functions.LOG_ERROR);
        lastNightInteractor.update();
        checkInForUpdates();
    }

    @Override
    public void onBackPressed() {
        if (progressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState, final PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastNightInteractor.update();
        if (shouldUpdateAlerts()) {
            alertsInteractor.update();
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
            selectTab(SOUNDS_ICON_KEY);
        } else if (intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {
            dispatchNotification(intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tabLayout != null) {
            tabLayout.clearOnTabSelectedListeners();
        }
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

    private void selectTab(final int position) {
        if (tabLayout == null) {
            return;
        }
        final TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab == null) {
            return;
        }
        tab.select();
    }

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.currentItemIndex = savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX,
                                                              viewPagerDelegate.getStartingItemPosition());
        } else {
            this.currentItemIndex = viewPagerDelegate.getStartingItemPosition();
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

    private void bindAlert(@NonNull final Alert alert) {
        if (shouldShow(alert)) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
            SystemAlertDialogFragment.newInstance(alert,
                                                  getResources())
                                     .showAllowingStateLoss(getFragmentManager(),
                                                            R.id.activity_new_home_bottom_alert_container,
                                                            BottomAlertDialogFragment.TAG);
        } else if (shouldUpdateDeviceIssues()) {
            deviceIssuesPresenter.update();
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

        deviceIssuesPresenter.updateLastShown(issue);
    }

    //endregion

    //region Alert Action Handler

    @Override
    public void unMuteSense() {
        showProgressOverlay(true);
        voiceSettingsInteractor.setSenseId(preferencesInteractor.getString(PreferencesInteractor.PAIRED_SENSE_ID,
                                                                           VoiceSettingsInteractor.EMPTY_ID));
        track(voiceSettingsInteractor.setMuted(false)
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
        progressOverlay.post(() -> {
            if (show) {
                progressOverlay.bringToFront();
                spinner.startSpinning();
                progressOverlay.setVisibility(View.VISIBLE);
            } else {
                spinner.stopSpinning();
                progressOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void setUpTabs(final boolean shouldSelect) {
        drawables[TRENDS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_trends_24);
        drawablesActive[TRENDS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_trends_active_24);
        drawables[INSIGHTS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_insight_24);
        drawablesActive[INSIGHTS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_insight_active_24);
        drawables[SOUNDS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_sound_24);
        drawablesActive[SOUNDS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_sound_active_24);
        drawables[CONDITIONS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_sense_24);
        drawablesActive[CONDITIONS_ICON_KEY] = ContextCompat.getDrawable(this, R.drawable.icon_sense_active_24);

        final SleepScoreIconDrawable.Builder drawableBuilder = new SleepScoreIconDrawable.Builder(this);
        drawableBuilder.withSize(drawables[TRENDS_ICON_KEY].getIntrinsicWidth(), drawables[TRENDS_ICON_KEY].getIntrinsicHeight());
        if (lastNightInteractor.timeline.hasValue()) {
            updateSleepScoreTab(lastNightInteractor.timeline.getValue());
        } else {
            drawables[SLEEP_ICON_KEY] = drawableBuilder.build();
            drawablesActive[SLEEP_ICON_KEY] = drawableBuilder.withSelected(true).build();
        }
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setIcon(drawables[SLEEP_ICON_KEY]));
        tabLayout.addTab(tabLayout.newTab().setIcon(drawables[TRENDS_ICON_KEY]));
        tabLayout.addTab(tabLayout.newTab().setIcon(drawables[INSIGHTS_ICON_KEY]));
        tabLayout.addTab(tabLayout.newTab().setIcon(drawables[SOUNDS_ICON_KEY]));
        tabLayout.addTab(tabLayout.newTab().setIcon(drawables[CONDITIONS_ICON_KEY]));
        tabLayout.addOnTabSelectedListener(new HomeTabListener());
        final TabLayout.Tab tab = tabLayout.getTabAt(currentItemIndex);
        if (shouldSelect && tab != null) {
            tab.setIcon(drawablesActive[currentItemIndex]);
            tab.select();
        }
    }

    private void updateSleepScoreTab(@Nullable final Timeline timeline) {
        final SleepScoreIconDrawable.Builder drawableBuilder = new SleepScoreIconDrawable.Builder(this);
        drawableBuilder.withSize(drawables[TRENDS_ICON_KEY].getIntrinsicWidth(), drawables[TRENDS_ICON_KEY].getIntrinsicHeight());
        if (timeline != null &&
                timeline.getScore() != null) {
            if (TimelineInteractor.hasValidCondition(timeline)) {
                drawableBuilder.withText(timeline.getScore());
            }
        }
        drawables[SLEEP_ICON_KEY] = drawableBuilder.build();
        drawablesActive[SLEEP_ICON_KEY] = drawableBuilder.withSelected(true).build();
        if (tabLayout == null) {
            return;
        }
        final TabLayout.Tab tab = tabLayout.getTabAt(SLEEP_ICON_KEY);
        if (tab == null) {
            return;
        }
        drawableBuilder.withSelected(tab.isSelected());
        tab.setIcon(drawableBuilder.build());
    }

    private void jumpToLastNight() {
        final TimelineFragment.Parent parent = getTimelineParent();
        if (parent != null) {
            parent.jumpToLastNight();
        }
    }

    @Override
    public TimelineFragment.Parent getTimelineParent() {
        return (TimelineFragment.Parent) getFragmentWithIndex(SLEEP_ICON_KEY);
    }

    @Override
    public FabPresenter getFabPresenter() {
        return (FabPresenter) getFragmentWithIndex(SOUNDS_ICON_KEY);
    }

    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment parentProvider = getFragmentWithIndex(INSIGHTS_ICON_KEY);
        if (parentProvider instanceof InsightInfoFragment.ParentProvider) {
            return ((InsightInfoFragment.ParentProvider) parentProvider).provideInsightInfoParent();
        } else {
            return null;
        }
    }

    @Nullable
    private Fragment getFragmentWithIndex(final int index) {
        return getFragmentManager()
                .findFragmentByTag(FragmentPagerAdapter.makeFragmentTag(R.id.activity_new_home_extended_view_pager, index));
    }

    //region Notifications

    private void dispatchNotification(@NonNull final Bundle notification) {
        stateSafeExecutor.execute(() -> {
            info(getClass().getSimpleName(), "dispatchNotification(" + notification + ")");

            final NotificationType target = Notification.typeFromBundle(notification);
            switch (target) {
                case SLEEP_SCORE: {
                    selectTab(SLEEP_ICON_KEY);
                    //todo support scrolling to date.

                    break;
                }
                case PILL_BATTERY: {
                    //todo handle and pass along
                }
            }
        });
    }

    //endregion


    public interface ScrollUp {
        void scrollUp();
    }

    private class HomeTabListener implements TabLayout.OnTabSelectedListener {

        @Override
        public void onTabSelected(final TabLayout.Tab tab) {
            if (!lastNightInteractor.timeline.hasValue()) {
                lastNightInteractor.update();
            }
            if (tab == null) {
                return;
            }
            currentItemIndex = tab.getPosition();
            tab.setIcon(drawablesActive[currentItemIndex]);
            if (currentItemIndex == SLEEP_ICON_KEY) {
                jumpToLastNight();
            }
        }

        @Override
        public void onTabUnselected(final TabLayout.Tab tab) {
            if (tab == null) {
                return;
            }
            tab.setIcon(drawables[tab.getPosition()]);
        }

        @Override
        public void onTabReselected(final TabLayout.Tab tab) {
            if (tab == null) {
                return;
            }
            final int position = tab.getPosition();
            final Fragment fragment = getFragmentWithIndex(position);
            if (fragment instanceof ScrollUp) {
                ((ScrollUp) fragment).scrollUp();
            }

        }

    }

    private static class HomeViewPagerDelegate extends BaseViewPagerPresenterDelegate {

        @NonNull
        @Override
        public StaticFragmentAdapter.Item[] getViewPagerItems() {
            return new StaticFragmentAdapter.Item[]{
                    new StaticFragmentAdapter.Item(TimelinePagerFragment.class, TimelinePagerFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(TrendsPresenterFragment.class, TrendsPresenterFragment.class.getSimpleName()),
                    new StaticFragmentAdapter.Item(HomePresenterFragment.class, HomePresenterFragment.class.getSimpleName()),
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