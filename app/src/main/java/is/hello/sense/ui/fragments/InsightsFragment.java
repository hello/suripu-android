package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProvider;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.InsightInfoActivity;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.adapter.ParallaxRecyclerScrollListener;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.ParallaxImageView;
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
    @Inject Picasso picasso;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;

    RecyclerView recyclerView;
    private @Nullable TutorialOverlayView tutorialOverlayView;

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
        progressBar = (ProgressBar)view.findViewById(R.id.fragment_insights_progress);

        final Resources resources = getResources();
        recyclerView = (RecyclerView) view.findViewById(R.id.fragment_insights_recycler);
        final int cardMargin = resources.getDimensionPixelSize(R.dimen.gap_outer_half);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new CardItemDecoration(cardMargin, cardMargin, resources.getDimensionPixelSize(R.dimen.gap_card_inter)));
        recyclerView.addOnScrollListener(new ParallaxRecyclerScrollListener());
        recyclerView.setItemAnimator(null);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources));

        this.insightsAdapter = new InsightsAdapter(getActivity(), dateFormatter, this, picasso);
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
                         this::bindInsights,
                         this::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.question,
                         this::bindQuestion,
                         this::questionUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (tutorialOverlayView != null) {
            tutorialOverlayView.dismiss(false);
            this.tutorialOverlayView = null;
        }

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
        Tutorial.TAP_INSIGHT_CARD.markShown(getActivity());

        // InsightsFragment lives inside of a child fragment manager,
        // which we don't want to use to display dialogs.
        //final FragmentManager fragmentManager = getActivity().getFragmentManager();
        final FragmentManager fragmentManager = getFragmentManager();
        final String imageUrl = insight.getImageUrl(getResources());

        if (insight.hasInfo()) {
            insightsAdapter.setLoadingInsightPosition(position);
            bindAndSubscribe(insightsPresenter.infoForInsight(insight), insightInfo -> {
                ParallaxImageView imageView = null;
                View child = recyclerView.getChildAt(position % recyclerView.getChildCount());
                if (child != null) {
                    InsightsAdapter.InsightViewHolder insightViewHolder = ((InsightsAdapter.InsightViewHolder)recyclerView.getChildViewHolder(child));
                    if (insightViewHolder != null) {
                        imageView = insightViewHolder.getImage();

                    }
                }


                InsightInfoActivity.startActivity(getActivity(),
                                                  insight.getTitle(),
                                                  insight.getMessage(),
                                                  insightInfo.getText(),
                                                  imageView,
                                                  imageUrl + position);

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
                                                          imageUrl,
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

    private void bindInsights(@NonNull List<Insight> insights) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.bindInsights(insights);

        final Activity activity = getActivity();
        if (tutorialOverlayView == null && Tutorial.TAP_INSIGHT_CARD.shouldShow(activity)) {
            this.tutorialOverlayView = new TutorialOverlayView(activity,
                                                               Tutorial.TAP_INSIGHT_CARD);
            tutorialOverlayView.setOnDismiss(() -> {
                this.tutorialOverlayView = null;
            });
            tutorialOverlayView.postShow(R.id.activity_home_container);
        }
    }

    private void insightsUnavailable(@Nullable Throwable e) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.insightsUnavailable(e);
    }

    //endregion


    //region Questions

    private void bindQuestion(@Nullable Question question){
        progressBar.setVisibility(View.GONE);
        insightsAdapter.bindQuestion(question);
    }


    private void questionUnavailable(@Nullable Throwable e){
        progressBar.setVisibility(View.GONE);
        insightsAdapter.questionUnavailable(e);
    }

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
