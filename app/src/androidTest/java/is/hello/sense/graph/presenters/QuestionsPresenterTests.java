package is.hello.sense.graph.presenters;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.api.model.Question;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

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
        SyncObserver<ArrayList<Question>> questions = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.questions);
        questions.await();

        assertNull(questions.getError());
        assertNotNull(questions.getLast());
    }

    public void testCurrentQuestion() throws Exception {
        SyncObserver<Question> question1 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.currentQuestion);
        question1.await();

        assertNull(question1.getError());
        assertNotNull(question1.getSingle());

        SyncObserver<Question> question2 = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.currentQuestion)
                                                       .ignore(1);
        presenter.nextQuestion();
        question2.await();

        assertNull(question2.getError());
        assertNull(question2.getLast());
    }

    public void testLogOutSideEffects() throws Exception {
        LocalBroadcastManager.getInstance(getInstrumentation().getContext())
                             .sendBroadcastSync(new Intent(ApiSessionManager.ACTION_LOGGED_OUT));


        SyncObserver<Question> currentQuestion = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.currentQuestion)
                                                             .ignore(1);
        currentQuestion.await();

        assertNull(currentQuestion.getError());
        assertNull(currentQuestion.getLast());


        SyncObserver<ArrayList<Question>> questions = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.questions)
                                                                  .ignore(1);
        questions.await();

        assertNull(questions.getError());
        assertEquals(Collections.<Question>emptyList(), questions.getLast());
    }
}
