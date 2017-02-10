package is.hello.sense.flows.home.ui.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.ui.views.HomeView;
import is.hello.sense.flows.home.ui.views.SenseTabLayout;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.Scope;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.AppUpdateDialogFragment;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.SystemAlertDialogFragment;
import is.hello.sense.util.Logger;

public class HomeFragment extends PresenterFragment<HomeView>
        implements SenseTabLayout.Listener,
        TimelineFragment.ParentProvider,
        OnBackPressedInterceptor {
    private static final String KEY_CURRENT_ITEM_INDEX = HomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static boolean isFirstActivityRun = true; // changed when paused
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;
    @Inject
    ApiService apiService;
    @Inject
    LastNightInteractor lastNightInteractor;
    @Inject
    UnreadStateInteractor unreadStateInteractor;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    AlertsInteractor alertsInteractor;
    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;
    private final HomeFragment.HomeViewPagerDelegate viewPagerDelegate = new HomeFragment.HomeViewPagerDelegate();

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new HomeView(getActivity(),
                                         getChildFragmentManager(),
                                         viewPagerDelegate,
                                         this);
        }
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof Scope) {
            this.deviceIssuesPresenter.bindScope((Scope) getActivity());
        } else {
            throw new IllegalStateException("A parent is required to control Scope");
        }
        addInteractor(this.deviceIssuesPresenter);
        addInteractor(this.alertsInteractor);
        addInteractor(this.lastNightInteractor);
        addInteractor(this.unreadStateInteractor);
        restoreState(savedInstanceState);
        if (shouldUpdateDeviceIssues()) {
            bindAndSubscribe(this.deviceIssuesPresenter.topIssue,
                             this::bindDeviceIssue,
                             Functions.LOG_ERROR);
        }
        bindAndSubscribe(this.alertsInteractor.alert,
                         this::bindAlert,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.lastNightInteractor.timeline,
                         this.presenterView::updateSleepScoreTab,
                         Functions.LOG_ERROR);
        bindAndSubscribe(this.unreadStateInteractor.hasUnreadItems,
                         this.presenterView::setUnreadItems,
                         Functions.LOG_ERROR);
        this.lastNightInteractor.update();
        this.unreadStateInteractor.update();
        checkInForUpdates();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (shouldUpdateAlerts()) {
            this.alertsInteractor.update();
        }
        this.lastNightInteractor.update();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFirstActivityRun = false;
    }

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
        this.presenterView.setCurrentItem(fragmentPosition);
        if (fragmentPosition == SenseTabLayout.SLEEP_ICON_KEY) {
            jumpToLastNight();
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


    @Nullable
    @Override
    public Timeline getCurrentTimeline() {
        if (this.lastNightInteractor.timeline.hasValue()) {
            return this.lastNightInteractor.timeline.getValue();
        }
        return null;
    }
    //endregion

    private boolean isShowingAlert() {
        return getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) != null
                || getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null;
    }

    private boolean shouldUpdateAlerts() {
        if (getActivity() instanceof OnboardingFlowProvider) {
            return ((OnboardingFlowProvider) getActivity()).getOnboardingFlow() != OnboardingActivity.FLOW_REGISTER;
        }
        return true;
    }

    private boolean shouldUpdateDeviceIssues() {
        return isFirstActivityRun && shouldUpdateAlerts();
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

    public void jumpToLastNight() {
        final TimelineFragment.Parent parent = getTimelineParent();
        if (parent != null) {
            parent.jumpToLastNight();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, this.presenterView.getSelectedTabPosition());
    }

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.presenterView.setUpTabs(false);
            this.presenterView.setCurrentItemIndex(savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX,
                                                                             this.viewPagerDelegate.getStartingItemPosition()));

        } else {
            this.presenterView.setUpTabs(true);
            this.presenterView.setCurrentItemIndex(this.viewPagerDelegate.getStartingItemPosition());
        }
    }

    @Nullable
    private Fragment getFragmentWithIndex(final int index) {
        return this.presenterView.getFragmentWithIndex(index);
    }

    public TimelineFragment.Parent getTimelineParent() {
        return (TimelineFragment.Parent) getFragmentWithIndex(SenseTabLayout.SLEEP_ICON_KEY);
    }

    @Nullable
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment parentProvider = getFragmentWithIndex(SenseTabLayout.INSIGHTS_ICON_KEY);
        if (parentProvider instanceof InsightInfoFragment.ParentProvider) {
            return ((InsightInfoFragment.ParentProvider) parentProvider).provideInsightInfoParent();
        } else {
            return null;
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (this.presenterView.isLoading()) {
            return true;
        }
        defaultBehavior.run();
        return true;
    }


    public static class HomeViewPagerDelegate extends BaseViewPagerPresenterDelegate {

        @NonNull
        @Override
        public StaticFragmentAdapter.Item[] getViewPagerItems() {
            return new StaticFragmentAdapter.Item[]{
                    new StaticFragmentAdapter.Item(TimelinePagerPresenterFragment.class, TimelinePagerPresenterFragment.class.getSimpleName()),
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
