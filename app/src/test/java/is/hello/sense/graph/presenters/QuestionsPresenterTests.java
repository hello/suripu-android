package is.hello.sense.graph.presenters;

import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

public class QuestionsPresenterTests extends InjectionTestCase {
    @Inject ApiSessionManager apiSessionManager;
    @Inject QuestionsPresenter presenter;

    @Before
    public void initialize() throws Exception {
        apiSessionManager.setSession(new OAuthSession());

        presenter.update();
    }

    @After
    public void tearDown() throws Exception {
        presenter.questions.forget();
        presenter.currentQuestion.forget();

        apiSessionManager.setSession(null);
    }

    @Test
    public void update() throws Exception {
        Sync.wrap(presenter.questions)
            .assertNotNull();
    }

    @Test
    public void currentQuestion() throws Exception {
        Sync.wrap(presenter.currentQuestion).await();

        Sync.wrap(presenter.currentQuestion)
            .assertNotNull();

        Question lastQuestion = Sync.wrap(presenter.currentQuestion).last();

        presenter.nextQuestion();
        Sync.wrap(presenter.currentQuestion.takeFirst(q -> q != lastQuestion))
            .assertNull();
    }

    @Test
    public void logOutSideEffects() throws Exception {
        Sync.wrap(presenter.questions).await();
        Sync.wrap(presenter.currentQuestion).await();

        presenter.onUserLoggedOut(new Intent(ApiSessionManager.ACTION_LOGGED_OUT));
        Sync.wrap(presenter.currentQuestion)
            .assertNull();

        Sync.wrap(presenter.questions)
            .assertTrue(Lists::isEmpty);
    }
}
