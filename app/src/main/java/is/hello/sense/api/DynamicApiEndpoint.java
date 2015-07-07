package is.hello.sense.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

public class DynamicApiEndpoint extends ApiEndpoint {
    public static final String PREF_CLIENT_ID_OVERRIDE = "debug_client_id_override";
    public static final String PREF_CLIENT_SECRET_OVERRIDE = "debug_client_secret_override";
    public static final String PREF_API_ENDPOINT_OVERRIDE = "debug_api_endpoint_override";

    private final SharedPreferences preferences;

    public DynamicApiEndpoint(@NonNull Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getUrl() {
        return preferences.getString(PREF_API_ENDPOINT_OVERRIDE, super.getUrl());
    }

    @Override
    public String getClientId() {
        return preferences.getString(PREF_CLIENT_ID_OVERRIDE, super.getClientId());
    }

    @Override
    public String getClientSecret() {
        return preferences.getString(PREF_CLIENT_SECRET_OVERRIDE, super.getClientSecret());
    }
}
