package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

public abstract class ApiSessionManager {
    //region Abstract

    public abstract void setOAuthSession(@Nullable OAuthSession session);
    public abstract @Nullable OAuthSession getOAuthSession();

    //endregion


    //region Accessors

    public boolean hasSession() {
        return getOAuthSession() != null;
    }

    public @Nullable String getAccessToken() {
        OAuthSession session = getOAuthSession();
        if (session != null) {
            return session.getAccessToken();
        } else {
            return null;
        }
    }

    //endregion
}
