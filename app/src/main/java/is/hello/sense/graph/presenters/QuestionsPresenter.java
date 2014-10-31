package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

@Singleton public final class QuestionsPresenter extends Presenter {
    private final ApiService apiService;

    public final PresenterSubject<List<Question>> questions = PresenterSubject.create();
    public final PresenterSubject<Question> currentQuestion = PresenterSubject.create();

    private PreferencesPresenter preferences;
    private int offset;

    private DateTime lastUpdated;


    //region Lifecycle

    @Inject public QuestionsPresenter(@NonNull ApiService apiService,
                                      @NonNull Context context,
                                      @NonNull PreferencesPresenter preferences) {
        this.apiService = apiService;
        this.preferences = preferences;

        Observable<Intent> logOutSignal = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent intent) {
        this.lastUpdated = null;
        setLastAcknowledged(null);

        questions.onNext(Collections.emptyList());
        currentQuestion.onNext(null);
    }

    @Override
    public void onRestoreState(@NonNull Parcelable savedState) {
        super.onRestoreState(savedState);

        if (savedState instanceof Bundle) {
            setOffset(((Bundle) savedState).getInt("offset"));
        }
    }

    @Override
    public @Nullable Parcelable onSaveState() {
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
        this.lastUpdated = null;

        return true;
    }

    //endregion


    //region Updating

    public boolean isUpdateTooSoon() {
        return lastUpdated != null && !lastUpdated.isAfter(lastUpdated.plusMinutes(2));
    }

    public void update() {
        if (isUpdateTooSoon()) {
            logEvent("redundant update requested, ignoring.");
            return;
        }

        if (isLastAcknowledgedBeforeToday()) {
            logEvent("loading today's questions");
            String timestamp = DateTime.now().toString("yyyy-MM-dd");
            apiService.questions(timestamp)
                      .subscribe(questions -> {
                          this.questions.onNext(questions);
                          this.lastUpdated = DateTime.now();
                          updateCurrentQuestion();
                      }, questions::onError);
        } else {
            logEvent("questions already updated today");
            this.questions.onNext(Collections.emptyList());
            this.currentQuestion.onNext(null);
        }
    }

    public void updateCurrentQuestion() {
        questions.take(1).subscribe(questions -> {
            if (offset < questions.size())
                currentQuestion.onNext(questions.get(offset));
            else
                currentQuestion.onNext(null);
        }, currentQuestion::onError);
    }

    //endregion


    //region Last update

    public boolean isLastAcknowledgedBeforeToday() {
        return getLastAcknowledged().isBefore(DateTime.now().withTimeAtStartOfDay());
    }

    public void setLastAcknowledged(@Nullable DateTime lastUpdated) {
        SharedPreferences.Editor transaction = preferences.edit();
        if (lastUpdated != null) {
            transaction.putString(PreferencesPresenter.QUESTIONS_LAST_ACKNOWLEDGED, lastUpdated.withTimeAtStartOfDay().toString());
        } else {
            transaction.remove(PreferencesPresenter.QUESTIONS_LAST_ACKNOWLEDGED);
        }
        transaction.apply();
    }

    public @NonNull DateTime getLastAcknowledged() {
        if (preferences.contains(PreferencesPresenter.QUESTIONS_LAST_ACKNOWLEDGED)) {
            String timestamp = preferences.getString(PreferencesPresenter.QUESTIONS_LAST_ACKNOWLEDGED, null);
            return DateTime.parse(timestamp);
        } else {
            return new DateTime(0, ISOChronology.getInstance());
        }
    }

    public void questionsAcknowledged() {
        this.lastUpdated = null;
        setLastAcknowledged(DateTime.now());
        update();
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

    public Observable<VoidResponse> answerQuestion(@NonNull Question.Choice answer) {
        return apiService.answerQuestion(answer);
    }

    public void skipQuestion() {
        currentQuestion.take(1).subscribe(question -> apiService.skipQuestion(question.getId()), Functions.LOG_ERROR);
        nextQuestion();
    }

    //endregion
}
