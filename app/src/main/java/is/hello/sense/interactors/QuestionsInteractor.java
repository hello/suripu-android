package is.hello.sense.interactors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.questions.ApiQuestionProvider;
import is.hello.sense.interactors.questions.QuestionProvider;
import is.hello.sense.interactors.questions.ReviewQuestionProvider;
import rx.Observable;
import rx.Scheduler;

@Singleton public class QuestionsInteractor extends Interactor {
    private static final String PROVIDER_STATE = "providerState";
    private static final String PROVIDER_NAME = "providerName";

    private final Context context;
    private final ApiService apiService;
    private final ApiSessionManager apiSessionManager;

    public final InteractorSubject<Question> question = InteractorSubject.create();

    @VisibleForTesting Source source;
    @VisibleForTesting QuestionProvider questionProvider;
    private final HashSet<Question.Choice> selectedChoices = new HashSet<>();


    //region Lifecycle

    @Inject public QuestionsInteractor(@NonNull Context context,
                                       @NonNull ApiService apiService,
                                       @NonNull ApiSessionManager apiSessionManager) {
        this.context = context;
        this.apiService = apiService;
        this.apiSessionManager = apiSessionManager;

        setSource(Source.API);

        final IntentFilter loggedOut = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        final Observable<Intent> logOutSignal = Rx.fromLocalBroadcast(context, loggedOut);
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent ignored) {
        question.onNext(null);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle savedState = questionProvider.saveState();
        if (savedState != null) {
            final Bundle wrapper = new Bundle();
            wrapper.putBundle(PROVIDER_STATE, savedState);
            wrapper.putString(PROVIDER_NAME, source.getName());
            return wrapper;
        } else {
            return null;
        }
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        final String providerName = savedState.getString(PROVIDER_NAME);
        final Bundle providerSavedState = savedState.getBundle(PROVIDER_STATE);
        if (providerName != null && providerSavedState != null) {
            final Source source = Source.fromName(providerName);
            if (source != null) {
                if (this.source != source) {
                    setSource(source);
                }

                questionProvider.restoreState(providerSavedState);
                question.onNext(questionProvider.getCurrentQuestion());

                return;
            }
        }

        if (!question.hasValue()) {
            question.onNext(null);
        }
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        if (questionProvider.lowMemory()) {
            question.forget();
            return true;
        } else {
            return false;
        }
    }

    //endregion


    //region Updating

    public void setSource(@NonNull Source source) {
        if (source != this.source) {
            this.source = source;
            this.questionProvider = source.createQuestionProvider(apiService,
                                                                  context,
                                                                  Rx.mainThreadScheduler());
        }
    }

    public void userEnteredFlow() {
        questionProvider.userEnteredFlow();
    }

    public final void update() {
        if (!apiSessionManager.hasSession()) {
            logEvent("skipping questions update, no api session.");
            return;
        }

        logEvent("Updating questions");
        questionProvider.prepare()
                        .subscribe(question);
    }

    public boolean hasQuestion() {
        return (question.hasValue() && question.getValue() != null);
    }

    //endregion


    //region Answering Questions

    public void addChoice(@NonNull Question.Choice choice) {
        selectedChoices.add(choice);
    }

    public void removeChoice(@NonNull Question.Choice choice) {
        selectedChoices.remove(choice);
    }

    public boolean hasSelectedChoices() {
        return !selectedChoices.isEmpty();
    }

    public void answerQuestion() {
        questionProvider.answerCurrent(new ArrayList<>(selectedChoices));
        selectedChoices.clear();
        question.onNext(questionProvider.getCurrentQuestion());
    }

    public Observable<Void> skipQuestion(boolean advanceImmediately) {
        selectedChoices.clear();
        Observable<Void> skip = questionProvider.skipCurrent(advanceImmediately);
        if (advanceImmediately) {
            return skip.doOnSubscribe(() -> {
                question.onNext(questionProvider.getCurrentQuestion());
            });
        } else {
            return skip.doOnNext(ignored -> {
                question.onNext(questionProvider.getCurrentQuestion());
            });
        }
    }

    //endregion


    public enum Source {
        API {
            @Override
            String getName() {
                return "ApiQuestionProvider";
            }

            @Override
            QuestionProvider createQuestionProvider(@NonNull ApiService apiService,
                                                    @NonNull Context context,
                                                    @NonNull Scheduler scheduler) {
                return new ApiQuestionProvider(apiService, scheduler);
            }
        },
        REVIEW {
            @Override
            String getName() {
                return "ReviewQuestionProvider";
            }

            @Override
            QuestionProvider createQuestionProvider(@NonNull ApiService apiService,
                                                    @NonNull Context context,
                                                    @NonNull Scheduler scheduler) {
                return new ReviewQuestionProvider(context,
                                                  apiService,
                                                  ReviewQuestionProvider.Destination.PlayStore);
            }
        },
        REVIEW_AMAZON {
            @Override
            String getName() {
                return "AmazonReviewQuestionProvider";
            }

            @Override
            QuestionProvider createQuestionProvider(@NonNull ApiService apiService,
                                                    @NonNull Context context,
                                                    @NonNull Scheduler scheduler) {
                return new ReviewQuestionProvider(context,
                                                  apiService,
                                                  ReviewQuestionProvider.Destination.Amazon);
            }
        },
        REVIEW_AMAZON_UK {
            @Override
            String getName() {
                return "AmazonUKReviewQuestionProvider";
            }

            @Override
            QuestionProvider createQuestionProvider(@NonNull final ApiService apiService,
                                                    @NonNull final Context context,
                                                    @NonNull final Scheduler scheduler) {
                return new ReviewQuestionProvider(context,
                                                  apiService,
                                                  ReviewQuestionProvider.Destination.AmazonUK);
            }
        };

        /**
         * Returns the name of the source, guaranteed to remain consistent across app versions.
         * @return  The name of the source.
         */
        abstract String getName();

        abstract QuestionProvider createQuestionProvider(final @NonNull ApiService apiService,
                                                         final @NonNull Context context,
                                                         final @NonNull Scheduler scheduler);

        /**
         * Searches for a <code>Source</code> matching the given name.
         * @param name  The name to match.
         * @return  The source if found; null otherwise.
         */
        static @Nullable Source fromName(@NonNull String name) {
            for (Source source : values()) {
                if (source.getName().equals(name)) {
                    return source;
                }
            }

            return null;
        }
    }
}
