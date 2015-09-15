package is.hello.sense.graph;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.model.Question;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.questions.QuestionProvider;
import is.hello.sense.util.Logger;
import is.hello.sense.util.markup.MarkupProcessor;
import rx.Observable;
import rx.schedulers.Schedulers;

public class FakeQuestionProvider implements QuestionProvider {
    private static final int CURRENT_NONE = -1;

    private final AssetManager assets;
    private final Gson gson;

    private final ArrayList<Question> questions = new ArrayList<>();
    private int current = CURRENT_NONE;

    //region Lifecycle

    public FakeQuestionProvider(@NonNull Context context) {
        this.assets = context.getAssets();
        this.gson = ApiModule.createConfiguredGson(new MarkupProcessor());
    }

    @Nullable
    @Override
    public Bundle saveState() {
        return null;
    }

    @Override
    public void restoreState(@NonNull Bundle savedState) {

    }

    @Override
    public boolean lowMemory() {
        return false;
    }

    //endregion


    //region Binding

    @Override
    public void userEnteredFlow() {
        // Don't care
    }

    private Observable<ArrayList<Question>> questions() {
        return Observable.<ArrayList<Question>>create(subscriber -> {
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
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<Question> prepare() {
        return Observable.<Question>create(subscriber -> {
            Observable<ArrayList<Question>> update = questions();
            update.observeOn(Rx.mainThreadScheduler())
                  .subscribe(questions -> {
                                 this.questions.clear();
                                 this.questions.addAll(questions);

                                 if (questions.isEmpty()) {
                                     this.current = CURRENT_NONE;
                                 } else {
                                     this.current = 0;
                                 }

                                 subscriber.onNext(getCurrentQuestion());
                                 subscriber.onCompleted();
                             },
                             error -> {
                                 this.questions.clear();
                                 this.current = CURRENT_NONE;

                                 subscriber.onError(error);
                             });
        }).subscribeOn(Schedulers.computation());
    }

    @Nullable
    @Override
    public Question getCurrentQuestion() {
        if (current == CURRENT_NONE) {
            return null;
        } else {
            return questions.get(current);
        }
    }

    @Override
    public void answerCurrent(@NonNull List<Question.Choice> choices) {
        Logger.debug(LOG_TAG, "answerCurrent(" + choices + ")");
        advance();
    }

    @Override
    public Observable<Void> skipCurrent(boolean advanceImmediately) {
        Logger.debug(LOG_TAG, "skipCurrent()");
        if (advanceImmediately) {
            advance();
            return Observable.just(null);
        } else {
            return Observable.<Void>create(subscriber -> {
                advance();

                subscriber.onNext(null);
                subscriber.onCompleted();
            }).observeOn(Rx.mainThreadScheduler());
        }
    }

    private void advance() {
        if (current == CURRENT_NONE) {
            return;
        }

        int next = current + 1;
        if (next >= questions.size()) {
            next = CURRENT_NONE;
        }
        this.current = next;
    }

    //endregion
}
