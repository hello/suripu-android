package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import net.hockeyapp.android.UpdateManager;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.notifications.NotificationType;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineNavigatorFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class HomeActivity
        extends InjectionActivity
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener, TimelineNavigatorFragment.OnTimelineDateSelectedListener
{
    public static final String EXTRA_IS_NOTIFICATION = HomeActivity.class.getName() + ".EXTRA_IS_NOTIFICATION";
    public static final String EXTRA_SHOW_UNDERSIDE = HomeActivity.class.getName() + ".EXTRA_SHOW_UNDERSIDE";

    @Inject QuestionsPresenter questionsPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject BuildValues buildValues;

    private long lastUpdated = Long.MAX_VALUE;

    private SlidingLayersView slidingLayersView;
    private FragmentPageView<TimelineFragment> viewPager;

    private boolean isFirstActivityRun;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.isFirstActivityRun = (savedInstanceState == null);
        if (savedInstanceState != null) {
            this.lastUpdated = savedInstanceState.getLong("lastUpdated");
        }

        // noinspection unchecked
        this.viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getFragmentManager());
        viewPager.setAdapter(this);
        viewPager.setOnTransitionObserver(this);
        if (viewPager.getCurrentFragment() == null) {
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight());
            viewPager.setCurrentFragment(fragment);
        }


        this.slidingLayersView = (SlidingLayersView) findViewById(R.id.activity_home_sliding_layers);
        slidingLayersView.setOnInteractionListener(this);
        slidingLayersView.setGestureInterceptingChild(viewPager);


        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EXTRA_IS_NOTIFICATION, false)) {
                onNotificationIntent(getIntent());
            }

            if (NotificationRegistration.shouldRegister(this)) {
                new NotificationRegistration(this).register();
            }
        }

        if (!buildValues.isDebugBuild() && buildValues.debugScreenEnabled) {
            UpdateManager.register(this, getString(R.string.build_hockey_id));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // This is probably not what we want to happen.
        Observable<Intent> onLogOut = fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        bindAndSubscribe(onLogOut, ignored -> {
            preferences
                    .edit()
                    .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, false)
                    .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE)
                    .apply();

            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }, Functions.LOG_ERROR);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("lastUpdated", lastUpdated);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!buildValues.isDebugBuild()) {
            UpdateManager.register(this, buildValues.hockeyId);
        }

        if (getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false)) {
            slidingLayersView.openWithoutAnimation();
        }

        if ((System.currentTimeMillis() - lastUpdated) > Constants.STALE_INTERVAL_MS && !isCurrentFragmentLastNight()) {
            Logger.info(getClass().getSimpleName(), "Timeline content stale, fast-forwarding to today.");
            TimelineFragment fragment = TimelineFragment.newInstance(DateFormatter.lastNight());
            viewPager.setCurrentFragment(fragment);


            Fragment navigatorFragment = getFragmentManager().findFragmentByTag(TimelineNavigatorFragment.TAG);
            if (navigatorFragment != null) {
                getFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        questionsPresenter.onTrimMemory(level);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getBooleanExtra(EXTRA_IS_NOTIFICATION, false)) {
            onNotificationIntent(intent);
        } else if (isFirstActivityRun && intent.getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false)) {
            slidingLayersView.openWithoutAnimation();
        }
    }

    protected void onNotificationIntent(@NonNull Intent intent) {
        Logger.info(HomeActivity.class.getSimpleName(), "HomeActivity received notification");

        NotificationType type = NotificationType.fromString(intent.getStringExtra(NotificationReceiver.EXTRA_TYPE));
        if (type == NotificationType.QUESTION && intent.hasExtra(NotificationReceiver.EXTRA_QUESTION_ID)) {
            answerQuestion();
        }
    }

    //endregion

    @Override
    public void onBackPressed() {
        if(slidingLayersView.isOpen()) {
            UndersideFragment undersideFragment = (UndersideFragment) getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
            if (undersideFragment != null && !undersideFragment.isAtStart()) {
                undersideFragment.jumpToStart();
            } else {
                slidingLayersView.close();
            }
        } else {
            super.onBackPressed();
        }
    }


    public boolean isCurrentFragmentLastNight() {
        TimelineFragment currentFragment = viewPager.getCurrentFragment();
        return (currentFragment != null && DateFormatter.isLastNight(currentFragment.getDate()));
    }


    //region Fragment Adapter

    @Override
    public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return true;
    }

    @Override
    public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().minusDays(1));
    }


    @Override
    public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        DateTime fragmentTime = fragment.getDate();
        return fragmentTime.isBefore(DateFormatter.lastNight().withTimeAtStartOfDay());
    }

    @Override
    public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().plusDays(1));
    }


    @Override
    public void onWillTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {

    }

    @Override
    public void onDidTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {
        this.lastUpdated = System.currentTimeMillis();

        fragment.onTransitionCompleted();
        Analytics.trackEvent(Analytics.EVENT_TIMELINE_ACTION, Analytics.createProperties(Analytics.PROP_TIMELINE_ACTION, Analytics.PROP_TIMELINE_ACTION_CHANGE_DATE));
    }

    //endregion


    //region Questions

    public void answerQuestion() {
        QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.show(getFragmentManager(), QuestionsDialogFragment.TAG);
    }

    //endregion


    //region Sliding Layers


    public SlidingLayersView getSlidingLayersView() {
        return slidingLayersView;
    }

    @Override
    public void onUserWillPullDownTopView() {
        if (getFragmentManager().findFragmentById(R.id.activity_home_underside_container) == null) {
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
        Fragment underside = getFragmentManager().findFragmentById(R.id.activity_home_underside_container);
        if (underside != null) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(underside)
                    .commit();
        }

        viewPager.getCurrentFragment().onUserDidPushUpTopView();
    }

    //endregion


    //region Timeline Navigation

    public void showTimelineNavigator(@NonNull DateTime startDate) {
        ViewGroup undersideContainer = (ViewGroup) findViewById(R.id.activity_home_content_container);

        TimelineNavigatorFragment navigatorFragment = TimelineNavigatorFragment.newInstance(startDate);
        navigatorFragment.show(getFragmentManager(), 0, TimelineNavigatorFragment.TAG);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();

        View view = navigatorFragment.getView();
        if (view == null) {
            throw new IllegalStateException();
        }
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (!navigatorFragment.isAdded() && !isDestroyed()) {
                    animate(viewPager)
                            .zoomInFrom(0.7f)
                            .addOnAnimationCompleted(finished -> {
                                if (finished) {
                                    undersideContainer.removeView(view);
                                }
                            })
                            .start();

                    fragmentManager.removeOnBackStackChangedListener(this);
                }
            }
        });

        undersideContainer.addView(view, 0);

        animate(viewPager)
                .zoomOutTo(View.GONE, 0.7f)
                .start();
    }

    @Override
    public void onTimelineDateSelected(@NonNull DateTime date) {
        if (!date.equals(viewPager.getCurrentFragment().getDate())) {
            viewPager.setCurrentFragment(TimelineFragment.newInstance(date));
        }
        getFragmentManager().popBackStack();
    }

    //endregion
}
