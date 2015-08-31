package is.hello.sense.graph.presenters.questions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class ApiQuestionProvider implements QuestionProvider {
    private final ApiService apiService;
    private final Scheduler updateScheduler;
    @VisibleForTesting final ArrayList<Question> questions = new ArrayList<>();
    @VisibleForTesting int current = 0;


    //region Lifecycle

    public ApiQuestionProvider(@NonNull ApiService apiService,
                               @NonNull Scheduler updateScheduler) {
        this.apiService = apiService;
        this.updateScheduler = updateScheduler;
    }

    @Override
    public String getName() {
        // Should remain the same across releases
        return "ApiQuestionProvider";
    }

    @Nullable
    @Override
    public Bundle saveState() {
        if (!questions.isEmpty()) {
            Bundle state = new Bundle();
            state.putSerializable("questions", questions);
            state.putInt("current", current);
            return state;
        } else {
            return null;
        }
    }

    @Override
    public void restoreState(@NonNull Bundle savedState) {
        if (this.questions.isEmpty()) {
            this.current = savedState.getInt("current", 0);

            @SuppressWarnings("unchecked")
            final ArrayList<Question> questions =
                    (ArrayList<Question>) savedState.getSerializable("questions");
            if (!Lists.isEmpty(questions)) {
                this.questions.addAll(questions);
            }
        }
    }

    @Override
    public boolean lowMemory() {
        this.questions.clear();
        this.current = 0;

        return true;
    }

    //endregion


    //region Binding

    @Override
    public void userEnteredFlow() {
        // Don't care
    }

    @VisibleForTesting
    Observable<ArrayList<Question>> latestQuestions() {
        String timestamp = DateTime.now().toString("yyyy-MM-dd");
        return apiService.questions(timestamp);
    }

    @Override
    public Observable<Question> prepare() {
        return Observable.<Question>create(subscriber -> {
            Observable<ArrayList<Question>> update = latestQuestions();
            update.observeOn(updateScheduler)
                  .subscribe(questions -> {
                                 this.questions.clear();
                                 this.questions.addAll(questions);

                                 this.current = 0;

                                 subscriber.onNext(getCurrentQuestion());
                                 subscriber.onCompleted();
                             },
                             error -> {
                                 this.questions.clear();
                                 this.current = 0;

                                 subscriber.onError(error);
                             });
        }).subscribeOn(Schedulers.computation());
    }

    @Nullable
    @Override
    public Question getCurrentQuestion() {
        if (current >= questions.size()) {
            return null;
        } else {
            return questions.get(current);
        }
    }

    @Override
    public void answerCurrent(@NonNull List<Question.Choice> choices) {
        if (current >= questions.size()) {
            return;
        }

        Question currentQuestion = questions.get(current);
        apiService.answerQuestion(currentQuestion.getAccountId(), choices)
                  .subscribe();

        this.current++;
    }

    @Override
    public Observable<Void> skipCurrent(boolean advanceImmediately) {
        if (current >= questions.size()) {
            return Observable.just(null);
        }

        Question currentQuestion = questions.get(current);
        Observable<VoidResponse> skip = apiService.skipQuestion(currentQuestion.getAccountId(),
                                                                currentQuestion.getId(),
                                                                "");
        if (advanceImmediately) {
            this.current++;
            return skip.map(Functions.TO_VOID);
        } else {
            return Observable.create(subscriber -> {
                skip.observeOn(updateScheduler)
                    .subscribe(ignored -> {
                                   this.current++;
                                   subscriber.onNext(null);
                               },
                               e -> {
                                   this.current++;
                                   subscriber.onError(e);
                               },
                               subscriber::onCompleted);
            });
        }
    }

    //endregion
}
