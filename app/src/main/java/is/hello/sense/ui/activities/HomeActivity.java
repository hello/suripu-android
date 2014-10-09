package is.hello.sense.ui.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static rx.android.observables.AndroidObservable.bindActivity;
import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class HomeActivity
        extends InjectionActivity
        implements FragmentPageView.Adapter<TimelineFragment>, FragmentPageView.OnTransitionObserver<TimelineFragment> {
    @Inject QuestionsPresenter questionsPresenter;

    private ViewGroup homeContainer;
    private ViewGroup newQuestionContainer;

    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.homeContainer = (ViewGroup) findViewById(R.id.activity_home_container);
        this.newQuestionContainer = (ViewGroup) findViewById(R.id.activity_home_new_question);

        Button newQuestionButton = (Button) newQuestionContainer.findViewById(R.id.activity_home_new_question_button);
        newQuestionButton.setOnClickListener(this::showQuestions);


        // noinspection unchecked
        FragmentPageView<TimelineFragment> viewPager = (FragmentPageView<TimelineFragment>) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getSupportFragmentManager());
        viewPager.setAdapter(this);
        viewPager.setOnTransitionObserver(this);
        if (viewPager.getCurrentFragment() == null) {
            TimelineFragment fragment = TimelineFragment.newInstance(DateTime.now());
            viewPager.setCurrentFragment(fragment);
        }
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
        }, ignored -> newQuestionContainer.setVisibility(View.INVISIBLE)));

        // This is probably not what we want to happen.
        Observable<Intent> logOut = bindActivity(this, fromLocalBroadcast(getApplicationContext(), new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT)));
        track(logOut.subscribe(unused -> finish()));
    }

    @Override
    protected void onStart() {
        super.onStart();

        questionsPresenter.update();
    }

    //endregion


    //region Fragment Adapter

    @Override
    public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return true;
    }

    @Override
    public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDateTime().minusDays(1));
    }


    @Override
    public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        DateTime fragmentTime = fragment.getDateTime();
        return fragmentTime.isBefore(DateTime.now().withTimeAtStartOfDay());
    }

    @Override
    public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDateTime().plusDays(1));
    }


    @Override
    public void onWillTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {

    }

    @Override
    public void onDidTransitionToFragment(@NonNull FragmentPageView<TimelineFragment> view, @NonNull TimelineFragment fragment) {
        fragment.onTransitionCompleted();
    }

    //endregion


    //region Questions

    public void showQuestionsButton() {
        if (newQuestionContainer.getVisibility() == View.VISIBLE)
            return;

        newQuestionContainer.setVisibility(View.VISIBLE);

        int containerHeight = homeContainer.getMeasuredHeight();
        int buttonHeight = newQuestionContainer.getMeasuredHeight();

        newQuestionContainer.setY((float) containerHeight);

        animate(newQuestionContainer)
                .y(containerHeight - buttonHeight)
                .setApplyChangesToView(true)
                .start();
    }

    public void hideQuestionsButton() {
        if (newQuestionContainer.getVisibility() == View.INVISIBLE)
            return;

        int containerHeight = homeContainer.getMeasuredHeight();
        int buttonHeight = newQuestionContainer.getMeasuredHeight();

        animate(newQuestionContainer)
                .y(containerHeight + buttonHeight)
                .setOnAnimationCompleted(finished -> newQuestionContainer.setVisibility(View.INVISIBLE))
                .start();
    }

    public void showQuestions(@NonNull View sender) {
        startActivity(new Intent(this, QuestionsActivity.class));
        hideQuestionsButton();
    }

    //endregion
}
