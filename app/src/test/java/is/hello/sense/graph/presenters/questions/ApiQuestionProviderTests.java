package is.hello.sense.graph.presenters.questions;

import android.os.Bundle;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.AssertExtensions.assertThrows;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class ApiQuestionProviderTests extends InjectionTestCase {
    @Inject ApiService apiService;
    private final ApiQuestionProvider questionProvider;

    public ApiQuestionProviderTests() {
        this.questionProvider = new ApiQuestionProvider(apiService, Schedulers.immediate());
    }

    @After
    public void tearDown() {
        questionProvider.questions.clear();
        questionProvider.current = ApiQuestionProvider.CURRENT_NONE;
    }

    public void prepareForTest() {
        Sync.last(questionProvider.prepare());
    }


    @Test
    public void saveState() throws Exception {
        prepareForTest();

        Bundle savedState = questionProvider.saveState();
        assertThat(savedState, is(notNullValue()));

        ApiQuestionProvider second = new ApiQuestionProvider(apiService, Rx.mainThreadScheduler());
        //noinspection ConstantConditions
        second.restoreState(savedState);

        assertThat(second.current, is(equalTo(questionProvider.current)));
        assertThat(second.questions, is(equalTo(questionProvider.questions)));
    }

    @Test
    public void lowMemory() throws Exception {
        prepareForTest();

        assertThat(questionProvider.questions, is(not(Collections.emptyList())));
        assertThat(questionProvider.current, is(not(equalTo(ApiQuestionProvider.CURRENT_NONE))));

        assertThat(questionProvider.lowMemory(), is(true));

        assertThat(questionProvider.questions, is(Collections.emptyList()));
        assertThat(questionProvider.current, is(equalTo(ApiQuestionProvider.CURRENT_NONE)));
    }

    @Test
    public void prepareSuccess() throws Exception {
        Question question = Sync.last(questionProvider.prepare());
        assertThat(question, is(equalTo(questionProvider.getCurrentQuestion())));
    }

    @Test
    public void prepareNoQuestions() throws Exception {
        ApiQuestionProvider provider = spy(questionProvider);
        doReturn(Observable.just(new ArrayList<>()))
                .when(provider)
                .latestQuestions();

        Question question = Sync.last(provider.prepare());
        assertThat(question, is(nullValue()));
        assertThat(question, is(equalTo(provider.getCurrentQuestion())));
    }

    @Test
    public void prepareError() throws Exception {
        ApiQuestionProvider provider = spy(questionProvider);
        doReturn(Observable.error(new Throwable("Oh noez")))
                .when(provider)
                .latestQuestions();

        assertThrows(() -> Sync.last(provider.prepare()));
        assertThat(provider.getCurrentQuestion(), is(nullValue()));
    }

    @Test
    public void answerQuestion() throws Exception {
        prepareForTest();

        Question firstToAnswer = questionProvider.getCurrentQuestion();
        @SuppressWarnings("ConstantConditions")
        List<Question.Choice> choices1 = Lists.newArrayList(firstToAnswer.getChoices().get(0));
        questionProvider.answerCurrent(choices1);

        Question secondToAnswer = questionProvider.getCurrentQuestion();
        assertThat(secondToAnswer, is(notNullValue()));
        assertThat(secondToAnswer, is(not(equalTo(firstToAnswer))));

        @SuppressWarnings("ConstantConditions")
        List<Question.Choice> choices2 = Lists.newArrayList(secondToAnswer.getChoices().get(0));
        questionProvider.answerCurrent(choices2);

        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }

    @Test
    public void skipQuestion() throws Exception {
        prepareForTest();

        Question firstToSkip = questionProvider.getCurrentQuestion();
        questionProvider.skipCurrent();

        Question secondToSkip = questionProvider.getCurrentQuestion();
        assertThat(secondToSkip, is(notNullValue()));
        assertThat(secondToSkip, is(not(equalTo(firstToSkip))));

        questionProvider.skipCurrent();

        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }
}
