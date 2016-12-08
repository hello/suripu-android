package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightType;
import is.hello.sense.flows.home.ui.activities.NewHomeActivity;
import is.hello.sense.graph.Scope;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.InsightsInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.QuestionsInteractor;
import is.hello.sense.interactors.questions.ReviewQuestionProvider;
import is.hello.sense.flows.home.ui.views.InsightsView;
import is.hello.sense.mvp.presenters.SubPresenterFragment;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Share;
import rx.Observable;


public class InsightsFragment extends SubPresenterFragment<InsightsView> implements
        SwipeRefreshLayout.OnRefreshListener,
        InsightsAdapter.InteractionListener,
        InsightInfoFragment.Parent,
        InsightsAdapter.OnRetry {

    @Inject
    InsightsInteractor insightsInteractor;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    DeviceIssuesInteractor deviceIssuesInteractor;
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    QuestionsInteractor questionsInteractor;
    @Inject
    Picasso picasso;
    @Inject
    ApiService apiService;


    @Nullable
    private TutorialOverlayView tutorialOverlayView;

    @Nullable
    private InsightsAdapter.InsightViewHolder selectedInsightHolder;

    @Nullable
    private Question currentQuestion;

    @NonNull
    private List<Insight> insights = Collections.emptyList();

    private boolean questionLoaded = false;
    private boolean insightsLoaded = false;
    private NewHomeActivity activity;

    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new InsightsView(getActivity(), dateFormatter, picasso, this);
        }
    }

    @Override
    public void onUserVisible() {
        Analytics.trackEvent(Analytics.Backside.EVENT_MAIN_VIEW, null);
        presenterView.updateWhatsNewState();
    }

    @Override
    public void onUserInvisible() {

    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(insightsInteractor);
        addInteractor(deviceIssuesInteractor);
        addInteractor(questionsInteractor);
        deviceIssuesInteractor.bindScope((Scope) getActivity());
        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(REVIEW_ACTION_RECEIVER,
                                               new IntentFilter(ReviewQuestionProvider.ACTION_COMPLETED));
        if (getActivity() instanceof NewHomeActivity) {
            activity = (NewHomeActivity) getActivity();
        }

    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setSwipeRefreshLayoutRefreshListener(this);

        // Combining these into a single Observable results in error
        // handling more or less breaking. Keep them separate until
        // we actually merge the endpoints on the backend.

        bindAndSubscribe(insightsInteractor.insights,
                         this::bindInsights,
                         this::insightsUnavailable);

        bindAndSubscribe(questionsInteractor.question,
                         this::bindQuestion,
                         this::questionUnavailable);
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();

        if (tutorialOverlayView != null) {
            tutorialOverlayView.dismiss(false);
            this.tutorialOverlayView = null;
        }

        insightsInteractor.unbindScope();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity())
                             .unregisterReceiver(REVIEW_ACTION_RECEIVER);
    }


    public final void update() {
        if (insightsInteractor.updateIfEmpty()) {
            presenterView.setRefreshing(true);
        }

        if (!questionsInteractor.hasQuestion()) {
            updateQuestion();
        }
    }


    //region Data Binding

    @OnboardingActivity.Flow
    protected int getOnboardingFlow() {
        final Activity activity = getActivity();
        if (activity instanceof NewHomeActivity) {
            return ((NewHomeActivity) activity).getOnboardingFlow();
        } else {
            return OnboardingActivity.FLOW_NONE;
        }
    }

    @Override
    public final void onRefresh() {
        fetchInsights();
    }

    /**
     * Pushes data into the adapter once both questions and insights have loaded.
     * <p>
     * Unfortunately this cannot be done through RxJava, if either the questions
     * or the insights request fails, the compound Observable will fail and stop
     * emitting values, even if the requests succeed on retry.
     */
    private void bindPendingIfReady() {
        if (!questionLoaded || !insightsLoaded) {
            return;
        }
        presenterView.showCards(currentQuestion, insights);

        final Activity activity = getActivity();
        if (getOnboardingFlow() == OnboardingActivity.FLOW_NONE &&
                tutorialOverlayView == null && Tutorial.TAP_INSIGHT_CARD.shouldShow(activity)) {
            this.tutorialOverlayView = new TutorialOverlayView(activity,
                                                               Tutorial.TAP_INSIGHT_CARD);
            tutorialOverlayView.setOnDismiss(() -> this.tutorialOverlayView = null);
            tutorialOverlayView.setAnchorContainer(getView());
            getAnimatorContext().runWhenIdle(() -> {
                if (tutorialOverlayView != null && getUserVisibleHint()) {
                    tutorialOverlayView.postShow(R.id.activity_home_container);
                }
            });
        }
    }

    private void bindInsights(@NonNull final List<Insight> insights) {
        this.insights = insights;
        this.insightsLoaded = true;
        bindPendingIfReady();
    }

    private void insightsUnavailable(@Nullable final Throwable e) {
        presenterView.insightsUnavailable(e, this);
    }

    private void bindQuestion(@Nullable final Question question) {
        // Prevent consecutive null question binds from causing redundant reloads.
        if (question == this.currentQuestion && questionLoaded) {
            return;
        }

        this.currentQuestion = question;
        this.questionLoaded = true;
        bindPendingIfReady();
    }

    private void questionUnavailable(@Nullable final Throwable e) {
        presenterView.questionsUnavailable(e);
    }

    //endregion


    //region Insights


    @Override
    @Nullable
    public final InsightInfoFragment.SharedState provideSharedState(final boolean isEnter) {
        if (selectedInsightHolder != null && getActivity() != null) {
            final InsightInfoFragment.SharedState state = new InsightInfoFragment.SharedState();
            Views.getFrameInWindow(selectedInsightHolder.itemView, state.cardRectInWindow);
            Views.getFrameInWindow(selectedInsightHolder.image, state.imageRectInWindow);
            state.imageParallaxPercent = selectedInsightHolder.image.getParallaxPercent();
            state.parentAnimator = presenterView.getAnimator();
            return state;
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public final Drawable getInsightImage() {
        if (selectedInsightHolder != null) {
            return selectedInsightHolder.image.getDrawable();
        } else {
            return null;
        }
    }

    @Override
    public final void onInsightClicked(@NonNull final InsightsAdapter.InsightViewHolder viewHolder) {
        final Insight insight = viewHolder.getInsight();
        if (insight.isError()) {
            return;
        }

        Analytics.trackEvent(Analytics.Backside.EVENT_INSIGHT_DETAIL, null);

        // InsightsFragment lives inside of a child fragment manager, whose root view is inset
        // on the bottom to make space for the open timeline. We go right to the root fragment
        // manager to keep things simple.
        final FragmentManager fragmentManager = getActivity().getFragmentManager();
        final InsightInfoFragment infoFragment = InsightInfoFragment.newInstance(insight,
                                                                                 getResources());
        infoFragment.show(fragmentManager,
                          R.id.activity_new_home_container,
                          InsightInfoFragment.TAG);

        this.selectedInsightHolder = viewHolder;
    }

    @Override
    public final void shareInsight(@NonNull final Insight insight) {
        showProgress(true);
        apiService.shareInsight(new InsightType(insight.getId()))
                  .doOnTerminate(() -> showProgress(false))
                  .subscribe(shareUrl -> {
                                 Share.text(shareUrl.getUrlForSharing(getActivity()))
                                      .withProperties(Share.getInsightProperties(insight.getCategory()))
                                      .send(getActivity());
                             },
                             throwable -> {
                                 final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(throwable, getActivity())
                                         .withTitle(R.string.error_share_insights_title)
                                         .withMessage(StringRef.from(R.string.error_share_insights_message))
                                         .build();
                                 errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
                             });
    }

    private void showProgress(final boolean show) {
        if (activity != null) {
            activity.showProgressOverlay(show);
        }
    }

    //endregion


    //region Questions

    public final void updateQuestion() {
        final Observable<Boolean> stageOne = deviceIssuesInteractor.latest().map(issue -> (issue == DeviceIssuesInteractor.Issue.NONE &&
                localUsageTracker.isUsageAcceptableForRatingPrompt() &&
                !preferencesInteractor.getBoolean(PreferencesInteractor.DISABLE_REVIEW_PROMPT, false)));
        stageOne.subscribe(showReview -> {
                               if (showReview) {
                                   final boolean reviewedOnAmazon = preferencesInteractor.getBoolean(PreferencesInteractor.HAS_REVIEWED_ON_AMAZON, false);
                                   // Amazon review links point to the first version of Sense and thus should not be shown for voice
                                   if (!reviewedOnAmazon && !preferencesInteractor.hasVoice()) {
                                       final String country = Locale.getDefault().getCountry();
                                       if (country.equalsIgnoreCase(Locale.US.getCountry())) {
                                           questionsInteractor.setSource(QuestionsInteractor.Source.REVIEW_AMAZON);
                                       } else if (country.equalsIgnoreCase(Locale.UK.getCountry())) {
                                           questionsInteractor.setSource(QuestionsInteractor.Source.REVIEW_AMAZON_UK);
                                       } // else, default source is API
                                   } else {
                                       questionsInteractor.setSource(QuestionsInteractor.Source.REVIEW);
                                   }
                               } // else, default source is API
                               questionsInteractor.update();
                           },
                           e -> {
                               Logger.warn(getClass().getSimpleName(),
                                           "Could not determine device status", e);
                               questionsInteractor.update();
                           });
    }

    @Override
    public final void onDismissLoadingIndicator() {
        presenterView.setRefreshing(false);
    }

    @Override
    public final void onSkipQuestion() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(questionsInteractor.skipQuestion(false),
                         ignored -> LoadingDialogFragment.close(getFragmentManager()),
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity()).build();
                             errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
                         });
    }

    @Override
    public final void onAnswerQuestion() {
        final QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.showAllowingStateLoss(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        presenterView.clearCurrentQuestion();
    }

    //endregion

    @Override
    public final void fetchInsights() {
        this.insights = Collections.emptyList();
        this.insightsLoaded = false;
        this.currentQuestion = null;
        this.questionLoaded = false;

        presenterView.setRefreshing(true);
        insightsInteractor.update();
        updateQuestion();
    }

    private final BroadcastReceiver REVIEW_ACTION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int response = intent.getIntExtra(ReviewQuestionProvider.EXTRA_RESPONSE,
                                                    ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY);
            switch (response) {
                case ReviewQuestionProvider.RESPONSE_WRITE_REVIEW:
                    stateSafeExecutor.execute(() -> UserSupport.showProductPage(getActivity()));
                    preferencesInteractor.edit()
                               .putBoolean(PreferencesInteractor.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;

                case ReviewQuestionProvider.RESPONSE_WRITE_REVIEW_AMAZON:
                    stateSafeExecutor.execute(() -> UserSupport.showAmazonReviewPage(getActivity(), "www.amazon.com"));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    preferencesInteractor.edit()
                               .putBoolean(PreferencesInteractor.HAS_REVIEWED_ON_AMAZON, true)
                               .apply();
                    break;

                case ReviewQuestionProvider.RESPONSE_WRITE_REVIEW_AMAZON_UK:
                    stateSafeExecutor.execute(() -> UserSupport.showAmazonReviewPage(getActivity(), "www.amazon.co.uk"));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    preferencesInteractor.edit()
                               .putBoolean(PreferencesInteractor.HAS_REVIEWED_ON_AMAZON, true)
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
                    preferencesInteractor.edit()
                               .putBoolean(PreferencesInteractor.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;
            }
        }
    };

}
