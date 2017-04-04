package is.hello.sense.util;

import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import is.hello.sense.interactors.BasePreferencesInteractor;
import is.hello.sense.interactors.PersistentPreferencesInteractor;

public class PersistentLocationManager {

    private static final String LOCATION_KEY = "last_known_location";

    private final BasePreferencesInteractor preferences;
    private final Gson gson;

    public PersistentLocationManager(@NonNull final PersistentPreferencesInteractor preferencesInteractor,
                                     @NonNull final Gson gson) {

        this.preferences = preferencesInteractor;
        this.gson = gson;
    }

    public void storeLocation(@Nullable final Location location) {
        final SharedPreferences.Editor editor = preferences.edit();
        if (location != null) {
            try {
                final String serializedValue = gson.toJson(location);
                editor.putString(LOCATION_KEY, serializedValue);
            } catch (final JsonSyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            editor.remove(LOCATION_KEY);
        }
        editor.apply();
    }

    @Nullable
    public Location retrieveLocation() {
        if (preferences.contains(LOCATION_KEY)) {
            final String serializedValue = preferences.getString(LOCATION_KEY, null);
            try {
                return gson.fromJson(serializedValue, Location.class);
            } catch (final JsonSyntaxException e) {
                Logger.error(PersistentLocationManager.class.getSimpleName(),
                             "Could not deserialize persisted location", e);
            }
        }

        return null;
    }

    public boolean hasLocation() {
        return preferences.contains(LOCATION_KEY);
    }
}
