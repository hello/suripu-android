package is.hello.sense.interactors;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;


import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.model.UserLocation;
import is.hello.sense.graph.annotations.PersistentSharedPreferences;
import is.hello.sense.util.InternalPrefManager;

@Singleton
public class PersistentPreferencesInteractor extends BasePreferencesInteractor {

    public static final String FIRMWARE_UPDATE_LAST_COMPLETED = "firmware_update_last_completed_with_device_id_";
    public static final String SENSE_VOICE_TUTORIAL_HAS_SEEN = "sense_voice_tutorial_has_seen_with_account_id_";
    public static final String NIGHT_MODE_SETTING = "night_mode_setting_with_account_id_";
    public static final String USER_LOCATION = "user_location_with_account_id_";

    private final Gson gson;

    public
    @Inject
    PersistentPreferencesInteractor(@NonNull final Context context,
                                    @NonNull @PersistentSharedPreferences final SharedPreferences sharedPreferences,
                                    @NonNull final Gson gson) {
        super(context, sharedPreferences);
        this.gson = gson;
    }

    public
    @Nullable
    DateTime getLastPillUpdateDateTime(final String deviceId) {
        if (contains(getLastUpdatedDeviceKey(deviceId))) {
            return new DateTime(getLong(getLastUpdatedDeviceKey(deviceId), 0));
        } else {
            return null;
        }
    }

    public void updateLastUpdatedDevice(@NonNull final String deviceId) {
        edit().putLong(
                getLastUpdatedDeviceKey(deviceId),
                DateTimeUtils.currentTimeMillis())
              .apply();
    }

    public boolean hasCompletedVoiceTutorial(@NonNull final String accountId) {
        return getBoolean(getHasSeenVoiceTutorialKey(accountId), false);
    }

    @Nullable
    public UserLocation getUserLocation() {
        final String serializedValue = getString(getUserLocationKey(), null);
        if (serializedValue == null) {
            return null;
        }
        return gson.fromJson(serializedValue, UserLocation.class);
    }

    public void saveUserLocation(@Nullable final UserLocation userLocation) {
        if (userLocation != null) {
            edit().putString(getUserLocationKey(), gson.toJson(userLocation)).apply();
        }
    }

    public void clearUserLocation() {
        edit().remove(getUserLocationKey()).apply();
    }

    @AppCompatDelegate.NightMode
    public int getCurrentNightMode() {
        switch (getInt(getNightModeSettingKey(), AppCompatDelegate.MODE_NIGHT_NO)) {
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                return AppCompatDelegate.MODE_NIGHT_AUTO;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    public void saveNightMode(@AppCompatDelegate.NightMode final int nightMode) {
        edit().putInt(getNightModeSettingKey(), nightMode)
              .apply();
    }

    public void setHasCompletedVoiceTutorial(@NonNull final String accountId, final boolean hasSeen) {
        edit().putBoolean(
                getHasSeenVoiceTutorialKey(accountId),
                hasSeen)
              .apply();
    }

    private String getLastUpdatedDeviceKey(@NonNull final String deviceId) {
        return PersistentPreferencesInteractor.FIRMWARE_UPDATE_LAST_COMPLETED + deviceId;
    }

    private String getHasSeenVoiceTutorialKey(@NonNull final String accountId) {
        return PersistentPreferencesInteractor.SENSE_VOICE_TUTORIAL_HAS_SEEN + accountId;
    }

    private String getNightModeSettingKey() {
        return PersistentPreferencesInteractor.NIGHT_MODE_SETTING + InternalPrefManager.getAccountId(getAppContext());
    }

    private String getUserLocationKey() {
        return PersistentPreferencesInteractor.USER_LOCATION + InternalPrefManager.getAccountId(getAppContext());
    }

}
