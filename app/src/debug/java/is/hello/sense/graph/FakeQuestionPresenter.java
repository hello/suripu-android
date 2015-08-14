package is.hello.sense.graph;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import rx.Observable;
import rx.schedulers.Schedulers;

public class FakeQuestionPresenter extends QuestionsPresenter {
    private final AssetManager assets;
    private final Gson gson;

    @Inject public FakeQuestionPresenter(@NonNull ApiService apiService,
                                         @NonNull ApiSessionManager apiSessionManager,
                                         @NonNull Context context,
                                         @NonNull Gson gson) {
        super(apiService, apiSessionManager, context);

        this.assets = context.getAssets();
        this.gson = gson;
    }

    @Override
    protected Observable<ArrayList<Question>> currentQuestions() {
        Observable<ArrayList<Question>> update = Observable.create(subscriber -> {
            InputStream questionsStream = null;
            try {
                questionsStream = assets.open("fake_questions.json");
                InputStreamReader reader = new InputStreamReader(questionsStream);
                ArrayList<Question> questions = gson.fromJson(reader, new TypeToken<ArrayList<Question>>(){}.getType());
                subscriber.onNext(questions);
                subscriber.onCompleted();
            } catch (IOException e) {
                subscriber.onError(e);
            } finally {
                Functions.safeClose(questionsStream);
            }
        });
        return update.subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<VoidResponse> answerQuestion(@NonNull Question question, @NonNull List<Question.Choice> answers) {
        logEvent("answerQuestion(" + question + ", " + answers + ")");
        return Observable.just(new VoidResponse());
    }
}
