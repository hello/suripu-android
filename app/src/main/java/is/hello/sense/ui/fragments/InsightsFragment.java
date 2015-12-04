package is.hello.sense.ui.fragments;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProvider;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
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
    @Inject DeviceIssuesPresenter deviceIssuesPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject QuestionsPresenter questionsPresenter;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceIssuesPresenter.bindScope(getScope());
        addPresenter(deviceIssuesPresenter);
        addPresenter(insightsPresenter);
        addPresenter(questionsPresenter);

        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(REVIEW_ACTION_RECEIVER,
                                               new IntentFilter(ReviewQuestionProvider.ACTION_COMPLETED));

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_MAIN_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_insights_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources));

        this.insightsAdapter = new InsightsAdapter(getActivity(), dateFormatter, this);
        recyclerView.setAdapter(insightsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Combining these into a single Observable results in error
        // handling more or less breaking. Keep them separate until
        // we actually merge the endpoints on the backend.

        bindAndSubscribe(insightsPresenter.insights,
                         insightsAdapter::bindInsights,
                         insightsAdapter::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.question,
                         insightsAdapter::bindQuestion,
                         insightsAdapter::questionUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        insightsPresenter.unbindScope();
        this.insightsAdapter = null;
        this.swipeRefreshLayout = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity())
                             .unregisterReceiver(REVIEW_ACTION_RECEIVER);
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
    public void onInsightClicked(int position, @NonNull Insight insight) {
        if (insight.isError()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_INSIGHT_DETAIL, null);

        // InsightsFragment lives inside of a child fragment manager,
        // which we don't want to use to display dialogs.
        final FragmentManager fragmentManager = getActivity().getFragmentManager();
        if (insight.hasInfo()) {
            insightsAdapter.setLoadingInsightPosition(position);
            bindAndSubscribe(insightsPresenter.infoForInsight(insight), insightInfo -> {
                final InsightInfoDialogFragment infoFragment =
                        InsightInfoDialogFragment.newInstance(insight.getTitle(),
                                                              insight.getMessage(),
                                                              // Replace this with the new image API
                                                              insightInfo.getImageUrl(),
                                                              insightInfo.getText());
                infoFragment.showAllowingStateLoss(fragmentManager, InsightInfoDialogFragment.TAG);

                insightsAdapter.setLoadingInsightPosition(RecyclerView.NO_POSITION);
            }, e -> {
                final ErrorDialogFragment errorDialogFragment =
                        new ErrorDialogFragment.Builder(e, getResources()).build();
                errorDialogFragment.showAllowingStateLoss(fragmentManager, ErrorDialogFragment.TAG);

                insightsAdapter.setLoadingInsightPosition(RecyclerView.NO_POSITION);
            });
        } else {
            final InsightInfoDialogFragment infoFragment =
                    InsightInfoDialogFragment.newInstance(insight.getTitle(),
                                                          insight.getMessage(),
                                                          // Replace this with the new image API
                                                          null,
                                                          null);
            infoFragment.showAllowingStateLoss(fragmentManager, InsightInfoDialogFragment.TAG);
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

    public void updateQuestion() {
        final Observable<Boolean> stageOne = deviceIssuesPresenter.latest().map(issue -> {
            return (issue == DeviceIssuesPresenter.Issue.NONE &&
                    localUsageTracker.isUsageAcceptableForRatingPrompt() &&
                    !preferences.getBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, false));
        });
        stageOne.subscribe(showReview -> {
                               if (showReview) {
                                   questionsPresenter.setSource(QuestionsPresenter.Source.REVIEW);
                               } else {
                                   questionsPresenter.setSource(QuestionsPresenter.Source.API);
                               }
                               questionsPresenter.update();
                           },
                           e -> {
                               Logger.warn(getClass().getSimpleName(),
                                           "Could not determine device status", e);
                               questionsPresenter.update();
                           });
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
                             ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getResources()).build();
                             errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
                         });
    }

    @Override
    public void onAnswerQuestion() {
        final QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.showAllowingStateLoss(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        insightsAdapter.clearCurrentQuestion();
    }

    //endregion


    private final BroadcastReceiver REVIEW_ACTION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int response = intent.getIntExtra(ReviewQuestionProvider.EXTRA_RESPONSE,
                                                    ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY);
            switch (response) {
                case ReviewQuestionProvider.RESPONSE_WRITE_REVIEW:
                    stateSafeExecutor.execute(() -> UserSupport.showProductPage(getActivity()));
                    preferences.edit()
                               .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;

                case ReviewQuestionProvider.RESPONSE_SEND_FEEDBACK:
                    stateSafeExecutor.execute(() -> UserSupport.showContactForm(getActivity()));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SHOW_HELP:
                    stateSafeExecutor.execute(() -> UserSupport.showUserGuide(getActivity()));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY:
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SUPPRESS_PERMANENTLY:
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    preferences.edit()
                               .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;
            }
        }
    };
}
