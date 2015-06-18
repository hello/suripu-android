package is.hello.sense.api.sessions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ApiSessionManagerTests {
    @Test
    public void facade() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager(InstrumentationRegistry.getContext());
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
