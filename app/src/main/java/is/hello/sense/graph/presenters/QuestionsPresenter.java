package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.presenters.questions.ApiQuestionProvider;
import is.hello.sense.graph.presenters.questions.QuestionProvider;
import rx.Observable;

@Singleton public class QuestionsPresenter extends Presenter {
    private static final String PROVIDER_STATE = "providerState";
    private static final String PROVIDER_NAME = "providerName";

    private final ApiService apiService;
    private final ApiSessionManager apiSessionManager;

    public final PresenterSubject<Question> question = PresenterSubject.create();

    private @NonNull QuestionProvider questionProvider;
    private final HashSet<Question.Choice> selectedChoices = new HashSet<>();


    //region Lifecycle

    @Inject public QuestionsPresenter(@NonNull Context context,
                                      @NonNull ApiService apiService,
                                      @NonNull ApiSessionManager apiSessionManager) {
        this.apiService = apiService;
        this.apiSessionManager = apiSessionManager;
        this.questionProvider = createApiQuestionProvider();

        IntentFilter loggedOut = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
        Observable<Intent> logOutSignal = Rx.fromLocalBroadcast(context, loggedOut);
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent ignored) {
        question.onNext(null);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle savedState = questionProvider.saveState();
        if (savedState != null) {
            Bundle wrapper = new Bundle();
            wrapper.putBundle(PROVIDER_STATE, savedState);
            wrapper.putString(PROVIDER_NAME, questionProvider.getName());
            return wrapper;
        } else {
            return null;
        }
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        String providerName = savedState.getString(PROVIDER_NAME);
        Bundle providerSavedState = savedState.getBundle(PROVIDER_STATE);
        if (TextUtils.equals(providerName, questionProvider.getName()) &&
                providerSavedState != null) {
            questionProvider.restoreState(providerSavedState);
            question.onNext(questionProvider.getCurrentQuestion());
        } else if (!question.hasValue()) {
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

    public ApiQuestionProvider createApiQuestionProvider() {
        return new ApiQuestionProvider(apiService, Rx.mainThreadScheduler());
    }

    public void setQuestionProvider(@NonNull QuestionProvider questionProvider) {
        this.questionProvider = questionProvider;
        update();
    }

    public @NonNull QuestionProvider getQuestionProvider() {
        return questionProvider;
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
}
