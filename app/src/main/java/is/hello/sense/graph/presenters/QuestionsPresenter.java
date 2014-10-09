package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Question;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Singleton public final class QuestionsPresenter extends Presenter {
    @Inject ApiService apiService;

    public final ReplaySubject<List<Question>> questions = ReplaySubject.create(1);
    public final ReplaySubject<Question> currentQuestion = ReplaySubject.create(1);

    private int offset;


    //region Lifecycle

    public QuestionsPresenter() {
        update();
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

    //endregion


    //region Updating

    @Override
    public void update() {
        String timestamp = DateTime.now().toString("yyyy-MM-dd");
        apiService.questions(timestamp)
                  .subscribe(questions -> {
                      this.questions.onNext(questions);
                      updateCurrentQuestion();
                  }, questions::onError);
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

    public Observable<ApiResponse> answerQuestion(@NonNull Question.Choice answer) {
        return apiService.answerQuestion(answer);
    }

    //endregion
}
