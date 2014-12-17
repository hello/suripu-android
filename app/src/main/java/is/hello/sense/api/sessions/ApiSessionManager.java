package is.hello.sense.api.sessions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;

import is.hello.sense.util.Analytics;

public abstract class ApiSessionManager {
    public static final String ACTION_LOGGED_OUT = ApiSessionManager.class.getName() + ".ACTION_LOGGED_OUT";

    //region Abstract

    protected abstract void storeOAuthSession(@Nullable OAuthSession session);
    protected abstract @Nullable OAuthSession retrieveOAuthSession();

    //endregion


    //region Accessors

    public final void setSession(@Nullable OAuthSession session) {
        storeOAuthSession(session);
        if (session != null) {
            Analytics.identify(session.getAccountId());
        }
    }

    public final @Nullable OAuthSession getSession() {
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
