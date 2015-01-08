package is.hello.sense.api.sessions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.InstrumentationTestCase;

import junit.framework.TestCase;

public class ApiSessionManagerTests extends InstrumentationTestCase {
    public void testFacade() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager(getInstrumentation().getContext());
        testApiSessionManager.setSession(testSession);

        assertTrue(testApiSessionManager.hasSession());
        assertNotNull(testApiSessionManager.getSession());

        assertTrue(testApiSessionManager.storeOAuthSessionCalled);
        assertTrue(testApiSessionManager.retrieveOAuthSessionCalled);
    }

    private static class TestApiSessionManager extends TransientApiSessionManager {
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
