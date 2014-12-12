package is.hello.sense.graph;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import rx.Observable;
import rx.schedulers.Schedulers;

public class FakeQuestionsPresenter extends QuestionsPresenter {
    private final Context context;
    private final ObjectMapper objectMapper;

    public FakeQuestionsPresenter(@NonNull ApiService apiService,
                                  @NonNull ApiSessionManager apiSessionManager,
                                  @NonNull Context context,
                                  @NonNull PreferencesPresenter preferences,
                                  @NonNull ObjectMapper objectMapper) {
        super(apiService, apiSessionManager, context, preferences);

        this.context = context;
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean isUpdateTooSoon() {
        return false;
    }

    @Override
    public boolean isLastAcknowledgedBeforeToday() {
        return true;
    }

    @Override
    public void setLastAcknowledged(@Nullable DateTime lastUpdated) {
        // Do nothing
    }

    @Override
    protected Observable<List<Question>> currentQuestions() {
        return Observable.create((Observable.OnSubscribe<List<Question>>) s -> {
            try {
                InputStream inputStream = context.getAssets().open("fake_questions.json");
                List<Question> questions = objectMapper.readValue(inputStream, new TypeReference<List<Question>>() {});
                s.onNext(questions);
                s.onCompleted();
            } catch (IOException e) {
                s.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<VoidResponse> answerQuestion(@NonNull Question question, @NonNull List<Question.Choice> answers) {
        return Observable.just(new VoidResponse());
    }

    @Override
    public void skipQuestion() {
        nextQuestion();
    }
}
