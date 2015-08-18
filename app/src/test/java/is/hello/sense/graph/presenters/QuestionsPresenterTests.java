package is.hello.sense.graph.presenters;

import android.content.Intent;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.presenters.questions.ApiQuestionProvider;
import is.hello.sense.graph.presenters.questions.QuestionProvider;
import is.hello.sense.util.Sync;
import rx.schedulers.Schedulers;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class QuestionsPresenterTests extends InjectionTestCase {
    @Inject ApiService apiService;
    @Inject ApiSessionManager apiSessionManager;
    @Inject QuestionsPresenter presenter;

    private QuestionProvider questionProvider;

    @Before
    public void setUp() throws Exception {
        this.questionProvider = spy(new ApiQuestionProvider(apiService, Schedulers.immediate()));
        apiSessionManager.setSession(new OAuthSession());

        presenter.setQuestionProvider(questionProvider);
    }

    @After
    public void tearDown() throws Exception {
        presenter.question.forget();

        apiSessionManager.setSession(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void saveStateMatchedNames() throws Exception {
        Sync.wrap(presenter.question)
            .assertThat(notNullValue());

        Bundle savedState = presenter.onSaveState();
        assertThat(savedState.size(), is(not(0)));
        assertThat(savedState, is(notNullValue()));

        presenter.question.forget();

        presenter.onRestoreState(savedState);

        Sync.wrap(presenter.question)
            .assertThat(notNullValue());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void saveStateMismatchedNames() throws Exception {
        Sync.wrap(presenter.question)
            .assertThat(notNullValue());

        doReturn("The class formerly known as ApiQuestionProvider")
                .when(questionProvider)
                .getName();

        Bundle savedState = presenter.onSaveState();
        assertThat(savedState.size(), is(not(0)));
        assertThat(savedState, is(notNullValue()));

        verify(questionProvider).getName();
        reset(questionProvider);

        presenter.question.forget();

        presenter.onRestoreState(savedState);

        Sync.wrap(presenter.question)
            .assertThat(nullValue());
    }

    @Test
    public void update() throws Exception {
        Sync.wrap(presenter.question)
            .assertThat(notNullValue());
    }

    @Test
    public void answerQuestion() throws Exception {
        Question firstQuestion = Sync.wrap(presenter.question)
                                     .assertThat(notNullValue());

        presenter.addChoice(firstQuestion.getChoices().get(0));
        presenter.answerQuestion();

        Question secondQuestion = Sync.wrap(presenter.question)
                                      .assertThat(allOf(notNullValue(),
                                                        not(equalTo(firstQuestion))));

        presenter.addChoice(secondQuestion.getChoices().get(0));
        presenter.answerQuestion();

        Sync.wrap(presenter.question)
            .assertThat(is(nullValue()));
    }

    @Test
    public void skipQuestion() throws Exception {
        Question firstQuestion = Sync.wrap(presenter.question)
                                     .assertThat(notNullValue());

        presenter.skipQuestion();

        Sync.wrap(presenter.question)
            .assertThat(allOf(notNullValue(), not(equalTo(firstQuestion))));

        presenter.answerQuestion();

        Sync.wrap(presenter.question)
            .assertThat(is(nullValue()));
    }

    @Test
    public void logOutSideEffects() throws Exception {
        Sync.wrap(presenter.question)
            .assertThat(notNullValue());

        Intent intent = new Intent(ApiSessionManager.ACTION_LOGGED_OUT);
        presenter.onUserLoggedOut(intent);

        Sync.wrap(presenter.question)
            .assertThat(nullValue());
    }
}
