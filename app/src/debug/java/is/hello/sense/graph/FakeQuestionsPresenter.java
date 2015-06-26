package is.hello.sense.graph;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import rx.Observable;
import rx.schedulers.Schedulers;

public class FakeQuestionsPresenter extends QuestionsPresenter {
    private final Context context;
    private final ObjectMapper objectMapper;

    public FakeQuestionsPresenter(@NonNull ApiService apiService,
                                  @NonNull ApiSessionManager apiSessionManager,
                                  @NonNull Context context,
                                  @NonNull ObjectMapper objectMapper) {
        super(apiService, apiSessionManager, context);

        this.context = context;
        this.objectMapper = objectMapper;
    }

    @Override
    protected Observable<ArrayList<Question>> currentQuestions() {
        return Observable.create((Observable.OnSubscribe<ArrayList<Question>>) s -> {
            try {
                InputStream inputStream = context.getAssets().open("fake_questions.json");
                ArrayList<Question> questions = objectMapper.readValue(inputStream, new TypeReference<ArrayList<Question>>() {});
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
