package is.hello.sense.graph.presenters.questions;

import android.os.Bundle;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
import static org.hamcrest.collection.IsEmptyCollection.empty;
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
        questionProvider.current = 0;
    }

    public void prepareForTest() {
        Sync.last(questionProvider.prepare());
    }


    @Test
    public void saveState() throws Exception {
        prepareForTest();

        Bundle savedState = questionProvider.saveState();
        assertThat(savedState, is(notNullValue()));

        ApiQuestionProvider second = new ApiQuestionProvider(apiService, Schedulers.immediate());
        //noinspection ConstantConditions
        second.restoreState(savedState);

        assertThat(second.current, is(equalTo(questionProvider.current)));
        assertThat(second.questions, is(equalTo(questionProvider.questions)));
        assertThat(second.getCurrentQuestion(), is(notNullValue()));
    }

    @Test
    public void saveStateAtEnd() {
        prepareForTest();
        questionProvider.skipCurrent(true).subscribe();
        questionProvider.skipCurrent(true).subscribe();

        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));

        Bundle savedState = questionProvider.saveState();
        assertThat(savedState, is(notNullValue()));

        ApiQuestionProvider second = new ApiQuestionProvider(apiService, Schedulers.immediate());
        //noinspection ConstantConditions
        second.restoreState(savedState);

        assertThat(second.current, is(equalTo(questionProvider.current)));
        assertThat(second.questions, is(equalTo(questionProvider.questions)));
        assertThat(second.getCurrentQuestion(), is(nullValue()));
    }

    @Test
    public void saveStateWithInterveningUpdate() {
        prepareForTest();

        Bundle savedState = questionProvider.saveState();
        assertThat(savedState, is(notNullValue()));

        ApiQuestionProvider second = new ApiQuestionProvider(apiService, Schedulers.immediate());
        Sync.last(second.prepare());
        questionProvider.skipCurrent(true).subscribe();

        //noinspection ConstantConditions
        second.restoreState(savedState);

        assertThat(second.current, is(not(equalTo(questionProvider.current))));
        assertThat(second.questions, is(not(equalTo(questionProvider.questions))));
    }

    @Test
    public void lowMemory() throws Exception {
        prepareForTest();

        questionProvider.skipCurrent(true).subscribe();

        assertThat(questionProvider.questions, is(not(empty())));
        assertThat(questionProvider.current, is(not(equalTo(0))));

        assertThat(questionProvider.lowMemory(), is(true));

        assertThat(questionProvider.questions, is(empty()));
        assertThat(questionProvider.current, is(equalTo(0)));
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
    public void skipQuestionAdvanceImmediately() throws Exception {
        prepareForTest();

        Question firstToSkip = questionProvider.getCurrentQuestion();
        questionProvider.skipCurrent(true).subscribe();

        Question secondToSkip = questionProvider.getCurrentQuestion();
        assertThat(secondToSkip, is(notNullValue()));
        assertThat(secondToSkip, is(not(equalTo(firstToSkip))));

        questionProvider.skipCurrent(true).subscribe();

        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }

    @Test
    public void skipQuestionAdvanceDeferred() throws Exception {
        prepareForTest();

        Question firstToSkip = questionProvider.getCurrentQuestion();
        Sync.last(questionProvider.skipCurrent(false));

        Question secondToSkip = questionProvider.getCurrentQuestion();
        assertThat(secondToSkip, is(notNullValue()));
        assertThat(secondToSkip, is(not(equalTo(firstToSkip))));

        Sync.last(questionProvider.skipCurrent(false));

        assertThat(questionProvider.getCurrentQuestion(), is(nullValue()));
    }
}
