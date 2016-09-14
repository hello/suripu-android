package is.hello.sense.interactors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateFormat;

import org.joda.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Observable;

@Singleton public class PreferencesInteractor extends BasePreferencesInteractor {
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

    public @Inject
    PreferencesInteractor(@NonNull Context context,
                          @NonNull @GlobalSharedPreferences SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);

        migrateIfNeeded();

        Observable<Intent> logOut = Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOut.subscribe(ignored -> clear(), Functions.LOG_ERROR);
    }

    //region Schema Migration

    @SuppressWarnings("deprecation")
    @VisibleForTesting boolean migrateIfNeeded() {
        int schemaVersion = getInt(SCHEMA_VERSION, SCHEMA_VERSION_1_0);
        if (schemaVersion < SCHEMA_VERSION_1_1) {
            if (contains(UNIT_SYSTEM__LEGACY)) {
                Logger.info(getClass().getSimpleName(), "Schema migration 1.0 -> 1.1");

                String unitSystem = getString(UNIT_SYSTEM__LEGACY, UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY);
                boolean useMetric = !UnitFormatter.LEGACY_UNIT_SYSTEM_US_CUSTOMARY.equals(unitSystem);
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
}

