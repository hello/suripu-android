package is.hello.sense.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import is.hello.sense.graph.presenters.PreferencesPresenter;

public class DynamicApiEndpoint extends ApiEndpoint {
    private final SharedPreferences preferences;

    public DynamicApiEndpoint(@NonNull Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getUrl() {
        return preferences.getString(PreferencesPresenter.DEBUG_API_URL_OVERRIDE, super.getUrl());
    }

    @Override
    public String getClientId() {
        return preferences.getString(PreferencesPresenter.DEBUG_CLIENT_ID_OVERRIDE, super.getClientId());
    }

    @Override
    public String getClientSecret() {
        return preferences.getString(PreferencesPresenter.DEBUG_CLIENT_SECRET_OVERRIDE, super.getClientSecret());
    }
}
