package is.hello.sense.graph.presenters.questions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import is.hello.sense.api.model.Question;
import rx.Observable;

public interface QuestionProvider {
    String LOG_TAG = QuestionProvider.class.getSimpleName();

    //region Lifecycle

    String getName();
    @Nullable Bundle saveState();
    void restoreState(@NonNull Bundle savedState);
    boolean lowMemory();

    //endregion

    //region Binding

    Observable<Question> prepare();
    @Nullable Question getCurrentQuestion();
    void answerCurrent(@NonNull List<Question.Choice> choices);
    void skipCurrent();

    //endregion
}
