package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.questions.ApiQuestionProvider;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProvider;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

public class InsightsFragment extends UndersideTabFragment
        implements SwipeRefreshLayout.OnRefreshListener, InsightsAdapter.InteractionListener {
    @Inject InsightsPresenter insightsPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject LocalUsageTracker localUsageTracker;
    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject QuestionsPresenter questionsPresenter;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(insightsPresenter);
        addPresenter(questionsPresenter);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_MAIN_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_insights_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new CardItemDecoration(getResources(), false));
        recyclerView.setItemAnimator(null);

        this.insightsAdapter = new InsightsAdapter(getActivity(), dateFormatter, this);
        recyclerView.setAdapter(insightsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<List<Insight>, Question>> data = Observable.combineLatest(insightsPresenter.insights,
                questionsPresenter.question, Pair::new);
        bindAndSubscribe(data,
                         insightsAdapter::bindData,
                         insightsAdapter::dataUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        insightsPresenter.unbindScope();
        this.insightsAdapter = null;
        this.swipeRefreshLayout = null;
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        if (!insightsPresenter.bindScope(getScope())) {
            swipeRefreshLayout.setRefreshing(true);
            insightsPresenter.update();
        }

        updateQuestion();
    }


    //region Insights

    @Override
    public void onInsightClicked(@NonNull Insight insight) {
        if (!Insight.CATEGORY_IN_APP_ERROR.equals(insight.getCategory())) {
            Analytics.trackEvent(Analytics.TopView.EVENT_INSIGHT_DETAIL, null);

            InsightInfoDialogFragment dialogFragment = InsightInfoDialogFragment.newInstance(insight);
            dialogFragment.showAllowingStateLoss(getFragmentManager(), InsightInfoDialogFragment.TAG);
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        insightsPresenter.update();
        updateQuestion();
    }

    //endregion


    //region Questions

    private void updateQuestionFromApi() {
        if (!(questionsPresenter.getQuestionProvider() instanceof ApiQuestionProvider)) {
            questionsPresenter.setQuestionProvider(questionsPresenter.createApiQuestionProvider());
        } else {
            questionsPresenter.update();
        }
    }

    private void updateQuestionForReview() {
        if (!(questionsPresenter.getQuestionProvider() instanceof ReviewQuestionProvider)) {
            questionsPresenter.setQuestionProvider(new ReviewQuestionProvider(getResources(),
                                                                              new ReviewTriggers()));
        }
    }

    public void updateQuestion() {
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Observable<Boolean> stageOne = devicesPresenter.latestTopIssue().map(issue -> {
                return (issue == DevicesPresenter.Issue.NONE &&
                        localUsageTracker.isUsageAcceptableForRatingPrompt() &&
                        !preferences.getBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, false));
            });
            stageOne.subscribe(showReview -> {
                                   if (showReview) {
                                       updateQuestionForReview();
                                   } else {
                                       updateQuestionFromApi();
                                   }
                               },
                               e -> {
                                   Logger.warn(getClass().getSimpleName(),
                                               "Could not determine device status", e);
                                   questionsPresenter.update();
                               });
        } else {
            questionsPresenter.update();
        }
    }

    @Override
    public void onDismissLoadingIndicator() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onSkipQuestion() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(questionsPresenter.skipQuestion(false),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
                             errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
                         });
    }

    @Override
    public void onAnswerQuestion() {
        QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.showAllowingStateLoss(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        insightsAdapter.clearCurrentQuestion();
    }

    //endregion


    private class ReviewTriggers implements ReviewQuestionProvider.Triggers {
        @Override
        public void onWriteReview() {
            UserSupport.showProductPage(getActivity());

            preferences.edit()
                       .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                       .apply();
        }

        @Override
        public void onSendFeedback() {
            UserSupport.showContactForm(getActivity());
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        }

        @Override
        public void onShowHelp() {
            UserSupport.showUserGuide(getActivity());
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        }

        @Override
        public void onSuppressPrompt(boolean forever) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);

            if (forever) {
                preferences.edit()
                           .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                           .apply();
            }
        }
    }
}
