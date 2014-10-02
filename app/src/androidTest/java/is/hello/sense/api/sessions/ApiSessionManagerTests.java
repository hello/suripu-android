package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

import junit.framework.TestCase;

import rx.observables.BlockingObservable;

public class ApiSessionManagerTests extends TestCase {
    public void testSignal() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager();
        BlockingObservable<OAuthSession> session = BlockingObservable.from(testApiSessionManager.currentSession);
        testApiSessionManager.setSession(testSession);
        OAuthSession signalSession = session.latest().iterator().next();
        assertTrue(testSession == signalSession);
    }

    public void testFacade() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager();
        testApiSessionManager.setSession(testSession);

        assertTrue(testApiSessionManager.hasSession());
        assertNotNull(testApiSessionManager.getSession());

        assertTrue(testApiSessionManager.storeOAuthSessionCalled);
        assertTrue(testApiSessionManager.retrieveOAuthSessionCalled);
    }

    private static class TestApiSessionManager extends TransientApiSessionManager {
        boolean storeOAuthSessionCalled = false;
        boolean retrieveOAuthSessionCalled = false;

        @Override
        protected void storeOAuthSession(@Nullable OAuthSession session) {
            this.storeOAuthSessionCalled = true;
            super.storeOAuthSession(session);
        }

        @Nullable
        @Override
        protected OAuthSession retrieveOAuthSession() {
            this.retrieveOAuthSessionCalled = true;
            return super.retrieveOAuthSession();
        }
    }
}
