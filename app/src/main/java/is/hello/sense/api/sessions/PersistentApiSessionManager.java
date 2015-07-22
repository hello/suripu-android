package is.hello.sense.api.sessions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import is.hello.sense.util.Logger;

public class PersistentApiSessionManager extends ApiSessionManager {
    private static final String SHARED_PREFERENCES_NAME = "oauth_session";
    private static final String SESSION_KEY = "session";

    private final SharedPreferences preferences;
    private final Gson gson;

    public PersistentApiSessionManager(@NonNull Context context, @NonNull Gson gson) {
        super(context);

        this.preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        this.gson = gson;
    }

    @Override
    protected void storeOAuthSession(@Nullable OAuthSession session) {
        SharedPreferences.Editor editor = preferences.edit();
        if (session != null) {
            try {
                String serializedValue = gson.toJson(session);
                editor.putString(SESSION_KEY, serializedValue);
            } catch (JsonSyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            editor.remove(SESSION_KEY);
        }
        editor.apply();
    }

    @Override
    protected @Nullable OAuthSession retrieveOAuthSession() {
        if (preferences.contains(SESSION_KEY)) {
            String serializedValue = preferences.getString(SESSION_KEY, null);
            try {
                return gson.fromJson(serializedValue, OAuthSession.class);
            } catch (JsonSyntaxException e) {
                Logger.error(PersistentApiSessionManager.class.getSimpleName(), "Could not deserialize persisted session", e);
            }
        }

        return null;
    }
}
