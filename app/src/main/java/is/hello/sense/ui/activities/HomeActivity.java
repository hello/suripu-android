package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.hockeyapp.android.UpdateManager;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.notifications.NotificationType;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.ViewUtil;
import is.hello.sense.ui.fragments.HomeUndersideFragment;
import is.hello.sense.ui.fragments.QuestionsFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
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
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment>, SlidingLayersView.OnInteractionListener {
    private static final String TAG_ANSWER_QUESTION = QuestionsFragment.class.getSimpleName();

    public static final String EXTRA_IS_NOTIFICATION = HomeActivity.class.getName() + ".EXTRA_IS_NOTIFICATION";
    public static final String EXTRA_SHOW_UNDERSIDE = HomeActivity.class.getName() + ".EXTRA_SHOW_UNDERSIDE";

    @Inject QuestionsPresenter questionsPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject BuildValues buildValues;

    private ViewGroup rootContainer;
    private ViewGroup homeContentContainer;
    private SlidingLayersView slidingLayersView;
    private ViewGroup newQuestionContainer;
    private FragmentPageView<TimelineFragment> viewPager;

    private boolean isFirstActivityRun;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.isFirstActivityRun = (savedInstanceState == null);

        this.rootContainer = (ViewGroup) findViewById(R.id.activity_home_container);
        this.homeContentContainer = (ViewGroup) findViewById(R.id.activity_home_content_container);


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

        bindAndSubscribe(questionsPresenter.currentQuestion, currentQuestion -> {
            if (currentQuestion == null || isAnswerQuestionOpen())
                hideNewQuestion();
            else
                showNewQuestion(currentQuestion);
        }, ignored -> {
            if (newQuestionContainer != null) {
                homeContentContainer.removeView(newQuestionContainer);
                this.newQuestionContainer = null;
            }
        });

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
    protected void onResume() {
        super.onResume();

        questionsPresenter.update();

        if (!buildValues.isDebugBuild()) {
            UpdateManager.register(this, buildValues.hockeyId);
        }

        if (isFirstActivityRun && getIntent().getBooleanExtra(EXTRA_SHOW_UNDERSIDE, false)) {
            slidingLayersView.openWithoutAnimation();
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
            answerQuestion();
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

    public void showNewQuestion(@NonNull Question question) {
        if (newQuestionContainer == null) {
            int containerHeight = homeContentContainer.getMeasuredHeight();
            if (containerHeight == 0) {
                ViewUtil.observeNextLayout(rootContainer).subscribe(ignored -> showNewQuestion(question));
                return;
            }

            this.newQuestionContainer = (ViewGroup) getLayoutInflater().inflate(R.layout.sub_fragment_new_question, homeContentContainer, false);
            newQuestionContainer.setVisibility(View.INVISIBLE);

            Button skipQuestion = (Button) newQuestionContainer.findViewById(R.id.sub_fragment_new_question_skip);
            skipQuestion.setOnClickListener(ignored -> questionsPresenter.skipQuestion());

            Button answerQuestion = (Button) newQuestionContainer.findViewById(R.id.sub_fragment_new_question_answer);
            answerQuestion.setOnClickListener(ignored -> answerQuestion());

            ViewUtil.observeNextLayout(homeContentContainer).subscribe(ignored -> {
                int newQuestionContainerHeight = newQuestionContainer.getMeasuredHeight();

                newQuestionContainer.setY((float) containerHeight);
                newQuestionContainer.setVisibility(View.VISIBLE);

                animate(newQuestionContainer)
                        .y(containerHeight - newQuestionContainerHeight)
                        .setApplyChangesToView(true)
                        .start();
            });

            homeContentContainer.addView(newQuestionContainer);
        }

        TextView answerTitle = (TextView) newQuestionContainer.findViewById(R.id.sub_fragment_new_question_title);
        answerTitle.setText(question.getText());
    }

    public void hideNewQuestion() {
        if (newQuestionContainer == null)
            return;

        int containerHeight = homeContentContainer.getMeasuredHeight();
        int buttonHeight = newQuestionContainer.getMeasuredHeight();

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        layoutParams.bottomMargin = 0;
        viewPager.getParent().requestLayout();

        if (containerHeight == 0) {
            homeContentContainer.removeView(newQuestionContainer);
            this.newQuestionContainer = null;

            return;
        }

        animate(newQuestionContainer)
                .y(containerHeight + buttonHeight)
                .addOnAnimationCompleted(finished -> {
                    homeContentContainer.removeView(newQuestionContainer);
                    this.newQuestionContainer = null;
                })
                .start();
    }

    public boolean isAnswerQuestionOpen() {
        return (getFragmentManager().findFragmentByTag(TAG_ANSWER_QUESTION) != null);
    }

    public void answerQuestion() {
        getFragmentManager()
                .beginTransaction()
                .add(R.id.activity_home_container, new QuestionsFragment(), TAG_ANSWER_QUESTION)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(QuestionsFragment.BACK_STACK_NAME)
                .commit();

        hideNewQuestion();
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
}
