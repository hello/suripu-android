package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

import rx.subjects.ReplaySubject;

public abstract class ApiSessionManager {
    public final ReplaySubject<OAuthSession> currentSession = ReplaySubject.create(1);

    //region Abstract

    protected abstract void storeOAuthSession(@Nullable OAuthSession session);
    protected abstract @Nullable OAuthSession retrieveOAuthSession();
    protected void synchronizeState() {
        currentSession.onNext(retrieveOAuthSession());
    }

    //endregion


    //region Accessors

    public void setSession(@Nullable OAuthSession session) {
        storeOAuthSession(session);
        synchronizeState();
    }

    public @Nullable OAuthSession getSession() {
        return retrieveOAuthSession();
    }

    public boolean hasSession() {
        return retrieveOAuthSession() != null;
    }

    public @Nullable String getAccessToken() {
        OAuthSession session = getSession();
        if (session != null) {
            return session.getAccessToken();
        } else {
            return null;
        }
    }

    //endregion
}
