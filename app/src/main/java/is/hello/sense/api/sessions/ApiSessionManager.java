package is.hello.sense.api.sessions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public abstract class ApiSessionManager {
    public static final String ACTION_SESSION_INVALIDATED = ApiSessionManager.class.getName() + ".ACTION_SESSION_INVALIDATED";
    public static final String ACTION_LOGGED_OUT = ApiSessionManager.class.getName() + ".ACTION_LOGGED_OUT";

    protected final Context context;

    protected ApiSessionManager(@NonNull Context context) {
        this.context = context;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            private long timeOfLastReception = 0;

            @Override
            public void onReceive(Context context, Intent intent) {
                if ((System.currentTimeMillis() - timeOfLastReception) > 1000) {
                    onSessionInvalidated();
                    this.timeOfLastReception = System.currentTimeMillis();
                }
            }
        };
        LocalBroadcastManager.getInstance(context)
                             .registerReceiver(receiver, new IntentFilter(ACTION_SESSION_INVALIDATED));
    }

    //region Abstract

    protected abstract void storeOAuthSession(@Nullable OAuthSession session);
    protected abstract @Nullable OAuthSession retrieveOAuthSession();

    //endregion


    //region Accessors

    public final void setSession(@Nullable OAuthSession session) {
        storeOAuthSession(session);
        if (session != null) {
            Analytics.setUserId(session.getAccountId());
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


    protected void onSessionInvalidated() {
        Logger.warn(getClass().getSimpleName(), "Session invalidated, logging out.");
        logOut();
    }

    public void logOut() {
        setSession(null);
        LocalBroadcastManager.getInstance(context)
                             .sendBroadcast(new Intent(ACTION_LOGGED_OUT));
    }
}
