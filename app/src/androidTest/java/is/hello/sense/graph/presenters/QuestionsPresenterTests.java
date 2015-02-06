package is.hello.sense.graph.presenters;

import android.content.Intent;
import android.test.FlakyTest;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

public class QuestionsPresenterTests extends InjectionTestCase {
    @Inject ApiSessionManager apiSessionManager;
    @Inject QuestionsPresenter presenter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        apiSessionManager.setSession(new OAuthSession());

        presenter.update();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        presenter.questions.forget();
        presenter.currentQuestion.forget();

        apiSessionManager.setSession(null);
    }

    public void testUpdate() throws Exception {
        Sync.wrap(presenter.questions)
            .assertNotNull();
    }

    public void testCurrentQuestion() throws Exception {
        Sync.next(presenter.questions);

        Sync.wrap(presenter.currentQuestion)
            .assertNotNull();

        presenter.nextQuestion();
        Sync.wrap(presenter.currentQuestion)
            .assertNull();
    }

    public void testLogOutSideEffects() throws Exception {
        presenter.onUserLoggedOut(new Intent(ApiSessionManager.ACTION_LOGGED_OUT));
        Sync.wrap(presenter.currentQuestion)
            .assertNull();

        Sync.wrap(presenter.questions)
            .assertTrue(Lists::isEmpty);
    }
}
