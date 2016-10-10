package is.hello.sense.interactors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateFormat;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import org.joda.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.model.UserFeatures;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Observable;

@Singleton
public class PreferencesInteractor extends BasePreferencesInteractor {
    public static final String SCHEMA_VERSION = "_schema_version";
    public static final int SCHEMA_VERSION_1_0 = 0;
    public static final int SCHEMA_VERSION_1_1 = 1;

    @Deprecated
    public static final String UNIT_SYSTEM__LEGACY = "unit_system_name";
    public static final String USE_24_TIME = "use_24_time";
    public static final String USE_CELSIUS = "use_celsius";
    public static final String USE_GRAMS = "use_grams";
    public static final String USE_CENTIMETERS = "use_centimeters";

    public static final String PUSH_ALERT_CONDITIONS_ENABLED = "push_alert_conditions_enabled";
    public static final String PUSH_SCORE_ENABLED = "push_score_enabled";

    public static final String ENHANCED_AUDIO_ENABLED = "enhanced_audio_enabled";

    public static final String PAIRED_DEVICE_ADDRESS = "paired_device_address";
    public static final String PAIRED_SENSE_ID = "paired_sense_id";
    public static final String PAIRED_PILL_ID = "paired_pill_id";

    public static final String ACCOUNT_CREATION_DATE = "account_creation_date";

    public static final String LAST_ONBOARDING_CHECK_POINT = "last_onboarding_check_point";
    public static final String ONBOARDING_COMPLETED = "onboarding_completed";

    public static final String HAS_UNREAD_INSIGHT_ITEMS = "has_unread_insight_items";
    public static final String DISABLE_REVIEW_PROMPT = "disable_review_prompt";
    public static final String HAS_REVIEWED_ON_AMAZON = "has_reviewed_on_amazon";

    public static final String SYSTEM_ALERT_LAST_SHOWN = "system_alert_last_shown";
    public static final String SENSE_ALERT_LAST_SHOWN = "sense_alert_last_shown";
    public static final String PILL_MISSING_ALERT_LAST_SHOWN = "pill_missing_alert_last_shown";
    public static final String PILL_FIRMWARE_UPDATE_ALERT_LAST_SHOWN = "pill_firmware_update_alert_last_shown";

    public static final String ROOM_CONDITIONS_WELCOME_CARD_TIMES_SHOWN = "room_conditions_welcome_card_times_shown";

    public static final String USER_FEATURES = "user_features";

    private final Gson gson;


    public
    @Inject
    PreferencesInteractor(@NonNull final Context context,
                          @NonNull final Gson gson,
                          @NonNull @GlobalSharedPreferences final SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
        this.gson = gson;

        migrateIfNeeded();

        final Observable<Intent> logOut = Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOut.subscribe(ignored -> clear(), Functions.LOG_ERROR);
    }

    //region Schema Migration

    @SuppressWarnings("deprecation")
    @VisibleForTesting
    boolean migrateIfNeeded() {
        final int schemaVersion = getInt(SCHEMA_VERSION, SCHEMA_VERSION_1_0);
        if (schemaVersion < SCHEMA_VERSION_1_1) {
            if (contains(UNIT_SYSTEM__LEGACY)) {
                Logger.info(getClass().getSimpleName(), "Schema migration 1.0 -> 1.1");

                final String unitSystem = getString(UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY);
                final boolean useMetric = !UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY.equals(unitSystem);
                edit().putBoolean(USE_CELSIUS, useMetric)
                      .putBoolean(USE_GRAMS, useMetric)
                      .putBoolean(USE_CENTIMETERS, useMetric)
                      .remove(UNIT_SYSTEM__LEGACY)
                      .putInt(SCHEMA_VERSION, SCHEMA_VERSION_1_1)
                      .apply();

                return true;
            }
        }

        return false;
    }

    //endregion

    //region Wrappers

    public boolean getUse24Time() {
        return getBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    public Observable<Boolean> observableUse24Time() {
        return observableBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    public LocalDate getAccountCreationDate() {
        final LocalDate savedCreationDate = getLocalDate(ACCOUNT_CREATION_DATE);
        if (savedCreationDate != null) {
            return savedCreationDate;
        } else {
            return Constants.TIMELINE_EPOCH;
        }
    }
    //endregion

    //region userFeatures helper
    public boolean hasVoice() {
        final UserFeatures userFeatures;
        try {
            userFeatures = gson.fromJson(getString(USER_FEATURES, null), UserFeatures.class);
        } catch (final JsonParseException e) {
            Logger.error(PreferencesInteractor.class.getName(), "could not deserialize user features", e);
            return false;

        }
        return contains(USER_FEATURES)
                && userFeatures != null
                && userFeatures.voice;
    }

    public void setFeatures(@Nullable final UserFeatures features) {
        final SharedPreferences.Editor editor = edit();
        if (features == null) {
            editor.remove(USER_FEATURES)
                  .apply();
            Logger.info(PreferencesInteractor.class.getName(), "cleared user features");
            return;
        }

        try {
            final String serializedFeatures = gson.toJson(features);
            editor.putString(USER_FEATURES, serializedFeatures)
                  .apply();
        } catch (final JsonSyntaxException e) {
            Logger.error(PreferencesInteractor.class.getName(), "could not serialize user features", e);
        }
    }
    //endregion
}

