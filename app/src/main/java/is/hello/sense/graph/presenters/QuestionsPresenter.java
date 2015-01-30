package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.schedulers.Schedulers;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

@Singleton public class QuestionsPresenter extends Presenter {
    private final ApiService apiService;
    private final ApiSessionManager apiSessionManager;

    public final PresenterSubject<ArrayList<Question>> questions = PresenterSubject.create();
    public final PresenterSubject<Question> currentQuestion = PresenterSubject.create();

    private int offset;


    //region Lifecycle

    @Inject public QuestionsPresenter(@NonNull ApiService apiService,
                                      @NonNull ApiSessionManager apiSessionManager,
                                      @NonNull Context context) {
        this.apiService = apiService;
        this.apiSessionManager = apiSessionManager;

        Observable<Intent> logOutSignal = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent intent) {
        questions.onNext(new ArrayList<>());
        currentQuestion.onNext(null);
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        setOffset(savedState.getInt("offset"));
    }

    @Override
    public @Nullable Bundle onSaveState() {
        Bundle savedState = new Bundle();
        savedState.putInt("offset", offset);
        return savedState;
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        questions.forget();
        currentQuestion.forget();

        return true;
    }

    //endregion


    //region Updating

    protected Observable<ArrayList<Question>> currentQuestions() {
        String timestamp = DateTime.now().toString("yyyy-MM-dd");
        return apiService.questions(timestamp);
    }

    public final void update() {
        if (!apiSessionManager.hasSession()) {
            logEvent("skipping questions update, no api session.");
            return;
        }

        logEvent("loading today's questions");
        currentQuestions()
                .observeOn(Schedulers.computation())
                .subscribe(questions -> {
                    this.questions.onNext(questions);
                    updateCurrentQuestion();
                }, e -> {
                    Logger.error(QuestionsPresenter.class.getSimpleName(), "Could not load questions.", e);

                    this.questions.onError(e);
                    this.currentQuestion.onError(e);
                });
    }

    public void updateCurrentQuestion() {
        questions.take(1)
                 .observeOn(Schedulers.computation())
                 .subscribe(questions -> {
                     if (offset < questions.size())
                         currentQuestion.onNext(questions.get(offset));
                     else
                         currentQuestion.onNext(null);
                 }, currentQuestion::onError);
    }

    //endregion


    //region Navigating Questions

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        updateCurrentQuestion();
    }

    public void nextQuestion() {
        setOffset(getOffset() + 1);
    }

    //endregion


    //region Answering Questions

    public Observable<VoidResponse> answerQuestion(@NonNull Question question, @NonNull List<Question.Choice> answers) {
        return apiService.answerQuestion(question.getAccountId(), answers);
    }

    public void skipQuestion() {
        currentQuestion.take(1)
                       .subscribe(question -> {
                                   apiService.skipQuestion(question.getAccountId(), question.getId())
                                             .subscribe(ignored -> logEvent("skipped question"),
                                                        Functions.LOG_ERROR);
                               },
                               Functions.LOG_ERROR);
        nextQuestion();
    }

    //endregion
}
