package is.hello.sense.api.sessions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApiSessionManagerTests extends SenseTestCase {
    @Test
    public void facade() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager(getContext());
        testApiSessionManager.setSession(testSession);

        assertTrue(testApiSessionManager.hasSession());
        assertNotNull(testApiSessionManager.getSession());

        assertTrue(testApiSessionManager.storeOAuthSessionCalled);
        assertTrue(testApiSessionManager.retrieveOAuthSessionCalled);
    }

    private static class TestApiSessionManager extends is.hello.sense.api.sessions.TestApiSessionManager {
        boolean storeOAuthSessionCalled = false;
        boolean retrieveOAuthSessionCalled = false;

        public TestApiSessionManager(@NonNull Context context) {
            super(context);
        }

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
