package is.hello.sense.graph.presenters.questions;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.Question.Choice;
import is.hello.sense.api.model.StoreReview;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class ReviewQuestionProvider implements QuestionProvider {
    public static final String ACTION_COMPLETED = ReviewQuestionProvider.class.getName() + ".ACTION_COMPLETED";
    public static final String EXTRA_RESPONSE = ReviewQuestionProvider.class.getName() + ".EXTRA_RESPONSE";
    public static final int RESPONSE_WRITE_REVIEW = 0;
    public static final int RESPONSE_SEND_FEEDBACK = 1;
    public static final int RESPONSE_SHOW_HELP = 2;
    public static final int RESPONSE_SUPPRESS_TEMPORARILY = 3;
    public static final int RESPONSE_SUPPRESS_PERMANENTLY = 4;
    public static final int RESPONSE_WRITE_REVIEW_AMAZON = 5;
    public static final int RESPONSE_WRITE_REVIEW_AMAZON_UK = 6;


    @VisibleForTesting static final long QUESTION_ID_NONE = -1;
    @VisibleForTesting static final long QUESTION_ID_INITIAL = 0;
    @VisibleForTesting static final long QUESTION_ID_GOOD = 1;
    @VisibleForTesting static final long QUESTION_ID_BAD = 2;

    private final Resources resources;
    private final LocalBroadcastManager localBroadcastManager;
    private final ApiService apiService;

    @VisibleForTesting Destination destination;

    private Question currentQuestion;
    @VisibleForTesting long currentQuestionId;

    //region Lifecycle

    public ReviewQuestionProvider(@NonNull Context context,
                                  @NonNull ApiService apiService,
                                  @NonNull Destination destination) {

        this.resources = context.getResources();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.apiService = apiService;
        this.destination = destination;

        Analytics.trackEvent(Analytics.StoreReview.SHOWN, null);
        setCurrentQuestionId(QUESTION_ID_INITIAL);
    }

    @Nullable
    @Override
    public Bundle saveState() {
        Bundle savedState = new Bundle();
        savedState.putLong("currentQuestionId", currentQuestionId);
        return savedState;
    }

    @Override
    public void restoreState(@NonNull Bundle savedState) {
        setCurrentQuestionId(savedState.getLong("currentQuestionId"));
    }

    @Override
    public boolean lowMemory() {
        this.currentQuestionId = QUESTION_ID_NONE;
        this.currentQuestion = null;

        return true;
    }

    public String getString(int id) throws Resources.NotFoundException {
        return resources.getString(id);
    }

    //endregion


    //region Questions

    @Override
    public void userEnteredFlow() {
        Analytics.trackEvent(Analytics.StoreReview.START, null);
    }

    @Override
    public Observable<Question> prepare() {
        return Observable.just(getCurrentQuestion());
    }

    void setCurrentQuestionId(long currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
        if (currentQuestionId == QUESTION_ID_NONE) {
            this.currentQuestion = null;
        } else if (currentQuestionId == QUESTION_ID_INITIAL) {
            List<Choice> choices = new ArrayList<>(3);
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_yes,
                                      getString(R.string.question_text_rating_prompt_initial_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_no,
                                      getString(R.string.question_text_rating_prompt_initial_no)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_initial_help,
                                      getString(R.string.question_text_rating_prompt_initial_help)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(R.string.question_text_rating_prompt_initial),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else if (currentQuestionId == QUESTION_ID_GOOD) {
            List<Choice> choices = new ArrayList<>(3);
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_yes,
                                      getString(R.string.question_text_rating_prompt_good_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_no,
                                      getString(R.string.question_text_rating_prompt_good_no)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_good_never,
                                      getString(R.string.question_text_rating_prompt_good_never)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(destination.questionTextId),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else if (currentQuestionId == QUESTION_ID_BAD) {
            List<Choice> choices = new ArrayList<>(2);
            choices.add(Choice.create(R.string.question_text_rating_prompt_bad_yes,
                                      getString(R.string.question_text_rating_prompt_bad_yes)));
            choices.add(Choice.create(R.string.question_text_rating_prompt_bad_no,
                                      getString(R.string.question_text_rating_prompt_bad_no)));
            this.currentQuestion = Question.create(currentQuestionId, 0,
                                                   resources.getString(R.string.question_text_rating_prompt_bad),
                                                   Question.Type.CHOICE,
                                                   DateTime.now(),
                                                   Question.AskTime.ANYTIME,
                                                   choices);
        } else {
            throw new IllegalArgumentException("Unknown question id");
        }
    }

    @Nullable
    @Override
    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    @Override
    public void answerCurrent(@NonNull List<Choice> choices) {
        int choiceId = (int) choices.get(0).getId();
        switch (choiceId) {
            // First screen
            case R.string.question_text_rating_prompt_initial_yes: {
                Analytics.trackEvent(Analytics.StoreReview.ENJOY_SENSE, null);
                setCurrentQuestionId(QUESTION_ID_GOOD);
                break;
            }
            case R.string.question_text_rating_prompt_initial_no: {
                Analytics.trackEvent(Analytics.StoreReview.DO_NOT_ENJOY_SENSE, null);
                setCurrentQuestionId(QUESTION_ID_BAD);
                break;
            }
            case R.string.question_text_rating_prompt_initial_help: {
                Analytics.trackEvent(Analytics.StoreReview.HELP_FROM_APP_REVIEW, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.HELP, false))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SHOW_HELP));
                break;
            }

            // Second screen (good)
            case R.string.question_text_rating_prompt_good_yes: {
                Analytics.trackEvent(destination.analyticsEvent, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.YES, true))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, destination.responseId));
                break;
            }
            case R.string.question_text_rating_prompt_good_no: {
                Analytics.trackEvent(Analytics.StoreReview.APP_REVIEW_COMPLETED_WITH_NO_ACTION, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.YES, false))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SUPPRESS_TEMPORARILY));
                break;
            }
            case R.string.question_text_rating_prompt_good_never: {
                Analytics.trackEvent(Analytics.StoreReview.DO_NOT_ASK_TO_RATE_APP_AGAIN, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.YES, false))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SUPPRESS_PERMANENTLY));
                break;
            }

            // Second screen (bad)
            case R.string.question_text_rating_prompt_bad_yes: {
                Analytics.trackEvent(Analytics.StoreReview.FEEDBACK_FROM_APP_REVIEW, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.NO, false))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SEND_FEEDBACK));
                break;
            }
            case R.string.question_text_rating_prompt_bad_no: {
                Analytics.trackEvent(Analytics.StoreReview.APP_REVIEW_COMPLETED_WITH_NO_ACTION, null);
                apiService.trackStoreReview(new StoreReview(StoreReview.Feedback.NO, false))
                          .subscribe();
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SUPPRESS_TEMPORARILY));
                break;
            }
        }
    }

    @Override
    public Observable<Void> skipCurrent(boolean advanceImmediately) {
        Analytics.trackEvent(Analytics.StoreReview.APP_REVIEW_SKIP, null);

        if (advanceImmediately) {
            setCurrentQuestionId(QUESTION_ID_NONE);
            localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                        .putExtra(EXTRA_RESPONSE, RESPONSE_SUPPRESS_TEMPORARILY));

            return Observable.just(null);
        } else {
            return Observable.<Void>create(subscriber -> {
                setCurrentQuestionId(QUESTION_ID_NONE);
                localBroadcastManager.sendBroadcast(new Intent(ACTION_COMPLETED)
                                                            .putExtra(EXTRA_RESPONSE, RESPONSE_SUPPRESS_TEMPORARILY));

                subscriber.onNext(null);
                subscriber.onCompleted();
            }).subscribeOn(Rx.mainThreadScheduler());
        }
    }

    //endregion

    public enum Destination {
        PlayStore (R.string.question_text_rating_prompt_good,
                   RESPONSE_WRITE_REVIEW,
                   Analytics.StoreReview.RATE_APP),

        Amazon (R.string.question_text_rating_prompt_good_amazon,
                RESPONSE_WRITE_REVIEW_AMAZON,
                Analytics.StoreReview.RATE_APP_AMAZON),

        AmazonUK(R.string.question_text_rating_prompt_good_amazon,
                 RESPONSE_WRITE_REVIEW_AMAZON_UK,
                 Analytics.StoreReview.RATE_APP_AMAZON);

        final int questionTextId;
        final int responseId;
        final String analyticsEvent;

        Destination(final int questionId,
                    final int responseId,
                    @NonNull final String analyticsEvent) {
            this.questionTextId = questionId;
            this.responseId = responseId;
            this.analyticsEvent = analyticsEvent;
        }
    }
}
