package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import net.hockeyapp.android.UpdateManager;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.notifications.NotificationType;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.HomeUndersideFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static rx.android.observables.AndroidObservable.bindActivity;
import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class HomeActivity
        extends InjectionActivity
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener {
    public static final String EXTRA_IS_NOTIFICATION = HomeActivity.class.getName() + ".EXTRA_IS_NOTIFICATION";

    @Inject QuestionsPresenter questionsPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject BuildValues buildValues;

    private ViewGroup homeContainer;
    private SlidingLayersView slidingLayersView;
    private Button newQuestionButton;
    private FragmentPageView<TimelineFragment> viewPager;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.homeContainer = (ViewGroup) findViewById(R.id.activity_home_container);

        this.newQuestionButton = (Button) findViewById(R.id.activity_home_new_question_button);
        newQuestionButton.setOnClickListener(this::showQuestions);


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

        if (!buildValues.isDebugBuild() && buildValues.debugScreenEnabled)
            UpdateManager.register(this, getString(R.string.build_hockey_id));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Observable<Boolean> noQuestions = bindActivity(this, questionsPresenter.questions.map(List::isEmpty));
        track(noQuestions.subscribe(none -> {
            if (none)
                hideQuestionsButton();
            else
                showQuestionsButton();
        }, ignored -> newQuestionButton.setVisibility(View.INVISIBLE)));

        // This is probably not what we want to happen.
        Observable<Intent> logOut = bindActivity(this, fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT)));
        track(logOut.subscribe(ignored -> {
            preferences
                    .edit()
                    .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, false)
                    .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE)
                    .apply();

            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();

        questionsPresenter.update();

        if (!buildValues.isDebugBuild()) {
            UpdateManager.register(this, buildValues.hockeyId);
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
        }
    }

    protected void onNotificationIntent(@NonNull Intent intent) {
        Logger.info(HomeActivity.class.getSimpleName(), "HomeActivity received notification");

        NotificationType type = NotificationType.fromString(intent.getStringExtra(NotificationReceiver.EXTRA_TYPE));
        if (type == NotificationType.QUESTION && intent.hasExtra(NotificationReceiver.EXTRA_QUESTION_ID)) {
            showQuestions(newQuestionButton);
        }
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
        fragment.onTransitionCompleted();
        Analytics.event(Analytics.EVENT_TIMELINE_ACTION, Analytics.createProperties(Analytics.PROP_TIMELINE_ACTION, Analytics.PROP_TIMELINE_ACTION_CHANGE_DATE));
    }

    //endregion


    //region Questions

    public void showQuestionsButton() {
        if (newQuestionButton.getVisibility() == View.VISIBLE)
            return;

        int containerHeight = homeContainer.getMeasuredHeight();
        if (containerHeight == 0) {
            homeContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    showQuestionsButton();
                    homeContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            return;
        }

        int buttonHeight = newQuestionButton.getMeasuredHeight();

        newQuestionButton.setY((float) containerHeight);
        newQuestionButton.setVisibility(View.VISIBLE);

        animate(newQuestionButton)
                .y(containerHeight - buttonHeight)
                .setApplyChangesToView(true)
                .addOnAnimationCompleted(finished -> {
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
                    layoutParams.bottomMargin = buttonHeight;
                    slidingLayersView.getParent().requestLayout();
                })
                .start();
    }

    public void hideQuestionsButton() {
        if (newQuestionButton.getVisibility() == View.INVISIBLE)
            return;

        int containerHeight = homeContainer.getMeasuredHeight();
        int buttonHeight = newQuestionButton.getMeasuredHeight();

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        layoutParams.bottomMargin = 0;
        viewPager.getParent().requestLayout();

        if (containerHeight == 0) {
            newQuestionButton.setVisibility(View.INVISIBLE);
            return;
        }

        animate(newQuestionButton)
                .y(containerHeight + buttonHeight)
                .addOnAnimationCompleted(finished -> newQuestionButton.setVisibility(View.INVISIBLE))
                .start();
    }

    public void showQuestions(@NonNull View sender) {
        startActivity(new Intent(this, QuestionsActivity.class));
        hideQuestionsButton();
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
                    .add(R.id.activity_home_underside_container, new HomeUndersideFragment())
                    .commit();
        }

        viewPager.getCurrentFragment().onUserWillPullDownTopView();
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
}
