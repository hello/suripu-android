package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ToggleButton;

import com.zendesk.logger.Logger;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsPresenterFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerFragment;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.presenters.HomePresenterFragment;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.mvp.presenters.TrendsPresenterFragment;
import is.hello.sense.mvp.util.FabPresenter;
import is.hello.sense.mvp.util.FabPresenterProvider;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.BottomAlertDialogFragment;
import is.hello.sense.ui.dialogs.DeviceIssueDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observable;
import rx.functions.Func0;

import static is.hello.sense.flows.home.ui.activities.HomeActivity.EXTRA_ONBOARDING_FLOW;
import static is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor.EMPTY_ID;

/**
 * Will eventually replace {@link HomeActivity}
 */

public class NewHomeActivity extends ScopedInjectionActivity
        implements SelectorView.OnSelectionChangedListener,
        FragmentNavigation,
        TimelineFragment.ParentProvider,
        FabPresenterProvider,
        Alert.ActionHandler {

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

    private static final String KEY_CURRENT_ITEM_INDEX = NewHomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static final int DEFAULT_ITEM_INDEX = 2;
    private SelectorView bottomSelectorView;
    private FragmentNavigationDelegate fragmentNavigationDelegate;
    private final FragmentMapper fragmentMapper = new FragmentMapper();
    private int currentItemIndex;
    private boolean isFirstActivityRun;
    private View progressOverlay;
    private SpinnerImageView spinner;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceIssuesPresenter.bindScope(this);
        addInteractor(deviceIssuesPresenter);
        addInteractor(alertsInteractor);

        setContentView(R.layout.activity_new_home);
        restoreState(savedInstanceState);
        this.progressOverlay = findViewById(R.id.activity_new_home_progress_overlay);
        this.spinner = (SpinnerImageView) progressOverlay.findViewById(R.id.activity_new_home_spinner);
        this.fragmentNavigationDelegate = new FragmentNavigationDelegate(this,
                                                                         R.id.activity_new_home_backside_container,
                                                                         stateSafeExecutor);
        if (savedInstanceState != null) {
            this.fragmentNavigationDelegate.onRestoreInstanceState(savedInstanceState);
        }
        this.bottomSelectorView = (SelectorView) findViewById(R.id.activity_new_home_bottom_selector_view);
        if (getActionBar() != null){
            getActionBar().hide();
        }
        initSelector(bottomSelectorView);

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

        if(shouldUpdateAlerts()) {
            bindAndSubscribe(alertsInteractor.alert,
                             this::bindAlert,
                             Functions.LOG_ERROR);

            alertsInteractor.update();
        }


    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, currentItemIndex);
        if (fragmentNavigationDelegate != null) {
            fragmentNavigationDelegate.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.bottomSelectorView != null) {
            this.bottomSelectorView.setOnSelectionChangedListener(null);
        }
        if (fragmentNavigationDelegate != null) {
            fragmentNavigationDelegate.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (progressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {

        setCurrentItemIndex(newSelectionIndex);

        final String tag = (String) this.bottomSelectorView.getButtonTagAt(newSelectionIndex);
        final Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment != getTopFragment() && !fragment.isRemoving()) {
            pushFragment(fragment, null, false); //todo save most recent 3 fragments in backstack
        } else if (fragment == null || fragment.isRemoving()) {
            pushFragment(fragmentMapper.getFragmentFromTag(tag), null, false);
        } else {
            Logger.d(NewHomeActivity.class.getName(), fragment + " is already visible");
        }
    }

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.currentItemIndex = savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX, DEFAULT_ITEM_INDEX);
        } else {
            this.currentItemIndex = DEFAULT_ITEM_INDEX;
        }
    }

    @Override
    public void pushFragment(@NonNull final Fragment fragment,
                             @Nullable final String title,
                             final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                              @Nullable final String title,
                                              final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        this.fragmentNavigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {
        //todo
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return fragmentNavigationDelegate.getTopFragment();
    }

    @Override
    public TimelineFragment.Parent get() {
        return (TimelineFragment.Parent) getFragmentManager()
                .findFragmentByTag(fragmentMapper.tags[0]);
    }

    @Override
    public FabPresenter getFabPresenter(){
        return (FabPresenter) getFragmentManager().findFragmentByTag(fragmentMapper.tags[3]);
    }

    private void initSelector(@NonNull final SelectorView selectorView) {
        selectorView.setButtonLayoutParams(new SelectorView.LayoutParams(0, SelectorView.LayoutParams.MATCH_PARENT, 1));
        final Drawable sleepScoreIconUnSelected = new SleepScoreIconDrawable.Builder(NewHomeActivity.this)
                .withSize(getWindowManager())
                .withText("82") // todo set this.
                .build();
        final Drawable sleepScoreIconSelected = new SleepScoreIconDrawable.Builder(NewHomeActivity.this)
                .withSize(getWindowManager())
                .withText("82")  // todo set this.
                .withSelected(true)
                .build();

        //todo update icons and order
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.icon_trends_24,
                R.drawable.icon_insight_24,
                R.drawable.icon_sound_24,
                R.drawable.icon_sense_24,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.icon_trends_active_24,
                R.drawable.icon_insight_active_24,
                R.drawable.icon_sound_active_24,
                R.drawable.icon_sense_active_24,
        };

        final SpannableString inactive = createIconSpan("empty", sleepScoreIconUnSelected);
        final SpannableString active = createIconSpan("empty", sleepScoreIconSelected);
        final ToggleButton toggleButton = selectorView.addOption(active,
                                                                 inactive,
                                                                 false);
        toggleButton.setPadding(0, 0, 0, 0);

        for (int i = 0; i < inactiveIcons.length; i++) {
            final SpannableString inactiveContent = createIconSpan("empty",
                                                                   inactiveIcons[i]);
            final SpannableString activeContent = createIconSpan("empty",
                                                                 activeIcons[i]);
            final ToggleButton button = selectorView.addOption(activeContent,
                                                               inactiveContent,
                                                               false);
            button.setPadding(0, 0, 0, 0);
        }
        selectorView.setButtonTags((Object[]) fragmentMapper.tags);
        selectorView.setOnSelectionChangedListener(NewHomeActivity.this);
        selectorView.setSelectedIndex(getCurrentItemIndex());
        onSelectionChanged(getCurrentItemIndex());

    }

    private SpannableString createIconSpan(@NonNull final String title,
                                           @DrawableRes final int icon) {
        final SpannableString spannableString = new SpannableString(title);
        final ImageSpan imageSpan = new ImageSpan(this, icon);
        spannableString.setSpan(imageSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    private SpannableString createIconSpan(@NonNull final String title,
                                           final Drawable icon) {
        final SpannableString spannableString = new SpannableString(title);
        final ImageSpan imageSpan = new ImageSpan(this, Styles.drawableToBitmap(icon));
        spannableString.setSpan(imageSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    private int getCurrentItemIndex() {
        return this.currentItemIndex;
    }

    public void setCurrentItemIndex(final int currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
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

    public void bindAlert(@NonNull final Alert alert){
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
    public void unMuteSense(){
        showProgressOverlay(true);
        voiceSettingsInteractor.setSenseId(preferences.getString(PreferencesInteractor.PAIRED_SENSE_ID,
                                                                 EMPTY_ID));
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

    private static class FragmentMapper {

        //todo these are the tags FragmentNavigationDelegate uses when making transactions
        // heavy dependence on fragment.class.getSimpleName()
        private final String TIMELINE_TAG = TimelinePagerFragment.class.getSimpleName();
        private final String TRENDS_TAG = TrendsPresenterFragment.class.getSimpleName();
        private final String HOME_TAG = HomePresenterFragment.class.getSimpleName();
        private final String SOUNDS_TAG = SoundsPresenterFragment.class.getSimpleName();
        private final String CONDITIONS_TAG = RoomConditionsPresenterFragment.class.getSimpleName();

        final String[] tags = {
                TIMELINE_TAG,
                TRENDS_TAG,
                HOME_TAG,
                SOUNDS_TAG,
                CONDITIONS_TAG
        };

        private final ArrayMap<String, Func0<Fragment>> map;

        private FragmentMapper(){
            this.map = new ArrayMap<>(tags.length);
            map.put(TIMELINE_TAG, TimelinePagerFragment::new);
            map.put(TRENDS_TAG, TrendsPresenterFragment::new);
            map.put(SOUNDS_TAG, SoundsPresenterFragment::new);
            map.put(CONDITIONS_TAG, RoomConditionsPresenterFragment::new);
            map.put(HOME_TAG, HomePresenterFragment::new);
        }

        Fragment getFragmentFromTag(@NonNull final String tag) {
            if (map.containsKey(tag)) {
                return map.get(tag)
                          .call();
            } else {
                throw new IllegalStateException("no fragment mapped to tag " + tag);
            }
        }

    }
}
