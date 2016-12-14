package is.hello.sense.flows.home.ui.activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.view.View;


import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerFragment;
import is.hello.sense.mvp.presenters.HomePresenterFragment;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.presenters.TrendsPresenterFragment;
import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;
import rx.Observable;


public class HomeActivity extends ScopedInjectionActivity
        implements
        Alert.ActionHandler,
        TimelineFragment.ParentProvider,
        ViewPagerPresenter {
    public static final String EXTRA_NOTIFICATION_PAYLOAD = HomeActivity.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    public static final String EXTRA_ONBOARDING_FLOW = HomeActivity.class.getName() + ".EXTRA_ONBOARDING_FLOW";



    @Inject
    AlertsInteractor alertsInteractor;
    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;
    @Inject
    LastNightInteractor lastNightInteractor;

    private static final String KEY_CURRENT_ITEM_INDEX = HomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static final int DEFAULT_ITEM_INDEX = 2;
    private int currentItemIndex;
    private boolean isFirstActivityRun;
    private View progressOverlay;
    private SpinnerImageView spinner;
    private ExtendedViewPager extendedViewPager;
    private TabLayout tabLayout;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.deviceIssuesPresenter.bindScope(this);
        addInteractor(this.deviceIssuesPresenter);
        addInteractor(this.alertsInteractor);

        setContentView(R.layout.activity_new_home);
        restoreState(savedInstanceState);
        this.progressOverlay = findViewById(R.id.activity_new_home_progress_overlay);
        this.spinner = (SpinnerImageView) progressOverlay.findViewById(R.id.activity_new_home_spinner);
        this.extendedViewPager = (ExtendedViewPager) findViewById(R.id.activity_new_home_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.activity_new_home_tab_layout);
        this.tabLayout.setupWithViewPager(this.extendedViewPager);
        extendedViewPager.setAdapter(new StaticFragmentAdapter(getFragmentManager(), getViewPagerItems()));
        setUpTabs(null);
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

        if (shouldUpdateAlerts()) {
            bindAndSubscribe(alertsInteractor.alert,
                             this::bindAlert,
                             Functions.LOG_ERROR);

            alertsInteractor.update();
        }

        bindAndSubscribe(lastNightInteractor.timeline,
                         this::setUpTabs,
                         Functions.LOG_ERROR);
        lastNightInteractor.update();


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

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.currentItemIndex = savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX, DEFAULT_ITEM_INDEX);
        } else {
            this.currentItemIndex = DEFAULT_ITEM_INDEX;
        }
    }


    @OnboardingActivity.Flow
    public int getOnboardingFlow() {
        @OnboardingActivity.Flow
        final int flow =
                getIntent().getIntExtra(EXTRA_ONBOARDING_FLOW,
                                        OnboardingActivity.FLOW_NONE);
        return flow;
    }

    //region Device Issues and Alerts

    private boolean shouldUpdateAlerts() {
        return isFirstActivityRun && getOnboardingFlow() == OnboardingActivity.FLOW_NONE;
    }

    private boolean shouldUpdateDeviceIssues() {
        return isFirstActivityRun && getOnboardingFlow() == OnboardingActivity.FLOW_NONE;
    }

    public void bindAlert(@NonNull final Alert alert) {
        if (alert.isValid()
                && getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) == null) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SYSTEM_ALERT_SHOWN);
            BottomAlertDialogFragment.newInstance(alert,
                                                  getResources())
                                     .showAllowingStateLoss(getFragmentManager(),
                                                            BottomAlertDialogFragment.TAG);
        } else if (shouldUpdateDeviceIssues()) {
            deviceIssuesPresenter.update();
        }
    }

    public void bindDeviceIssue(@NonNull final DeviceIssuesInteractor.Issue issue) {
        if (issue == DeviceIssuesInteractor.Issue.NONE
                || getFragmentManager().findFragmentByTag(DeviceIssueDialogFragment.TAG) != null
                || getFragmentManager().findFragmentByTag(BottomAlertDialogFragment.TAG) != null) {
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

    //region Alert Action Handler

    @Override
    public void unMuteSense() {
        showProgressOverlay(true);
        voiceSettingsInteractor.setSenseId(preferences.getString(PreferencesInteractor.PAIRED_SENSE_ID,
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

    public void setUpTabs(@Nullable final Timeline timeline) {
        final SleepScoreIconDrawable.Builder drawableBuilder = new SleepScoreIconDrawable.Builder(this);
        drawableBuilder.withSize(getWindowManager());
        if (timeline != null &&
                timeline.getScoreCondition() != ScoreCondition.UNAVAILABLE &&
                timeline.getScore() != null) {
            drawableBuilder.withText(timeline.getScore());
        }


        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setIcon(drawableBuilder.build()));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_trends_24));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_insight_24));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_sound_24));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_sense_24));
        tabLayout.getTabAt(currentItemIndex);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
                currentItemIndex = tab.getPosition();
                final Drawable drawable = tab.getIcon();
                if (drawable == null) {
                    return;
                }
                drawable.setColorFilter(ContextCompat.getColor(HomeActivity.this, R.color.blue5), PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabUnselected(final TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
                final Drawable drawable = tab.getIcon();
                if (drawable == null) {
                    return;
                }
                drawable.setColorFilter(ContextCompat.getColor(HomeActivity.this, R.color.gray3), PorterDuff.Mode.MULTIPLY);
            }

            @Override
            public void onTabReselected(final TabLayout.Tab tab) {

            }
        });
    }

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
    public int getStartingItemPosition() {
        return DEFAULT_ITEM_INDEX;
    }

    @Override
    public TimelineFragment.Parent get() {
        return (TimelineFragment.Parent) getFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.activity_new_home_extended_view_pager + ":0");
    }
}
