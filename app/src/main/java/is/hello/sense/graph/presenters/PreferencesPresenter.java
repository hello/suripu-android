package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateFormat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

@Singleton public class PreferencesPresenter extends Presenter {
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
    public static final String PAIRED_DEVICE_SSID = "paired_device_ssid";
    public static final String PAIRED_SENSE_ID = "paired_sense_id";
    public static final String PAIRED_PILL_ID = "paired_pill_id";

    public static final String LAST_ONBOARDING_CHECK_POINT = "last_onboarding_check_point";
    public static final String ONBOARDING_COMPLETED = "onboarding_completed";

    public static final String DISABLE_REVIEW_PROMPT = "disable_review_prompt";


    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final AccountPresenter accountPresenter;

    /**
     * SharedPreferences only keeps a weak reference to its OnSharedPreferenceChangeListener,
     * so we have to keep a strong reference to them somewhere if we want updates.
     */
    private final Set<OnSharedPreferenceChangeListener> strongListeners = Collections.synchronizedSet(new HashSet<>());

    public @Inject PreferencesPresenter(@NonNull Context context,
                                        @NonNull @GlobalSharedPreferences SharedPreferences sharedPreferences,
                                        @NonNull AccountPresenter accountPresenter) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.accountPresenter = accountPresenter;

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

    
    //region Editing

    public SharedPreferences.Editor edit() {
        return sharedPreferences.edit();
    }

    public void clear() {
        logEvent("Clearing user preferences.");

        edit().clear()
              .apply();
    }

    public Observable<Void> pullAccountPreferences() {
        logEvent("Pulling preferences from backend");

        return accountPresenter.preferences().map(prefs -> {
            edit().putBoolean(PUSH_SCORE_ENABLED, prefs.pushScore)
                  .putBoolean(PUSH_ALERT_CONDITIONS_ENABLED, prefs.pushAlertConditions)
                  .putBoolean(ENHANCED_AUDIO_ENABLED, prefs.enhancedAudioEnabled)
                  .putBoolean(USE_CELSIUS, prefs.useCelsius)
                  .putBoolean(USE_GRAMS, prefs.useMetricWeight)
                  .putBoolean(USE_CENTIMETERS, prefs.useMetricHeight)
                  .putBoolean(USE_24_TIME, prefs.use24Time)
                  .apply();

            logEvent("Pulled preferences");

            return null;
        });
    }

    public void pushAccountPreferences() {
        logEvent("Pushing account preferences");

        Account.Preferences preferences = new Account.Preferences();
        boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        preferences.pushScore = getBoolean(PUSH_SCORE_ENABLED, true);
        preferences.pushAlertConditions = getBoolean(PUSH_ALERT_CONDITIONS_ENABLED, true);
        preferences.enhancedAudioEnabled = getBoolean(ENHANCED_AUDIO_ENABLED, false);
        preferences.use24Time = getUse24Time();
        preferences.useCelsius = getBoolean(USE_CELSIUS, defaultMetric);
        preferences.useMetricWeight = getBoolean(USE_GRAMS, defaultMetric);
        preferences.useMetricHeight = getBoolean(USE_CENTIMETERS, defaultMetric);
        accountPresenter.updatePreferences(preferences)
                        .subscribe(ignored -> logEvent("Pushed account preferences"),
                                   Functions.LOG_ERROR);
    }

    //endregion


    //region Instantaneous Values

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    //endregion


    //region Observable Values

    private <T> Observable<T> observableValue(@NonNull String key, @Nullable T defaultValue, Func2<String, T, T> producer) {
        return Observable.create(subscriber -> {
            OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key)) {
                    subscriber.onNext(producer.call(key, defaultValue));
                }
            };

            Subscription subscription = Subscriptions.create(() -> {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
                strongListeners.remove(changeListener);
            });
            subscriber.add(subscription);

            sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
            strongListeners.add(changeListener);
            subscriber.onNext(producer.call(key, defaultValue));
        });
    }

    public Observable<String> observeChangesOn(@NonNull String... keys) {
        return Observable.create(subscriber -> {
            OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                for (String key : keys) {
                    if (key.equals(changedKey)) {
                        subscriber.onNext(changedKey);
                    }
                }
            };

            Subscription subscription = Subscriptions.create(() -> {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
                strongListeners.remove(changeListener);
            });
            subscriber.add(subscription);

            sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
            strongListeners.add(changeListener);
        });
    }

    public Observable<String> observableString(@NonNull String key, @Nullable String defaultValue) {
        return observableValue(key, defaultValue, sharedPreferences::getString);
    }

    public Observable<Boolean> observableBoolean(@NonNull String key, boolean defaultValue) {
        return observableValue(key, defaultValue, sharedPreferences::getBoolean);
    }

    public Observable<Integer> observableInteger(@NonNull String key, int defaultValue) {
        return observableValue(key, defaultValue, sharedPreferences::getInt);
    }

    //endregion


    //region Wrappers

    public boolean getUse24Time() {
        return getBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    public Observable<Boolean> observableUse24Time() {
        return observableBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    //endregion
}

