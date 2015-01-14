package is.hello.sense.api.sessions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import is.hello.sense.util.Logger;

public class PersistentApiSessionManager extends ApiSessionManager {
    private static final String SHARED_PREFERENCES_NAME = "oauth_session";
    private static final String SESSION_KEY = "session";

    private final SharedPreferences preferences;
    private final ObjectMapper mapper;

    public PersistentApiSessionManager(@NonNull Context context, @NonNull ObjectMapper mapper) {
        super(context);

        this.preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        this.mapper = mapper;
    }

    @Override
    protected void storeOAuthSession(@Nullable OAuthSession session) {
        SharedPreferences.Editor editor = preferences.edit();
        if (session != null) {
            try {
                String serializedValue = mapper.writeValueAsString(session);
                editor.putString(SESSION_KEY, serializedValue);
            } catch (JsonProcessingException e) {
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
                return mapper.readValue(serializedValue, OAuthSession.class);
            } catch (IOException e) {
                Logger.error(PersistentApiSessionManager.class.getSimpleName(), "Could not deserialize persisted session", e);
            }
        }

        return null;
    }
}
