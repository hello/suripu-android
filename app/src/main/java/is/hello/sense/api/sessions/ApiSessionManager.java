package is.hello.sense.api.sessions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

public abstract class ApiSessionManager {
    public static final String ACTION_LOGGED_OUT = ApiSessionManager.class.getName() + ".ACTION_LOGGED_OUT";

    //region Abstract

    protected abstract void storeOAuthSession(@Nullable OAuthSession session);
    protected abstract @Nullable OAuthSession retrieveOAuthSession();

    //endregion


    //region Accessors

    public void setSession(@Nullable OAuthSession session) {
        storeOAuthSession(session);
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

    public void logOut(@NonNull Context context) {
        setSession(null);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(ACTION_LOGGED_OUT));
    }
}
