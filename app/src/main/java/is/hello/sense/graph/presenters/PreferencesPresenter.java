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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    public static final String PAIRED_DEVICE_ADDRESS = "paired_device_address";
    public static final String PAIRED_DEVICE_SSID = "paired_device_ssid";
    public static final String PAIRED_SENSE_ID = "paired_sense_id";
    public static final String PAIRED_PILL_ID = "paired_pill_id";

    public static final String LAST_ONBOARDING_CHECK_POINT = "last_onboarding_check_point";
    public static final String ONBOARDING_COMPLETED = "onboarding_completed";

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
            edit().putBoolean(USE_CELSIUS, Account.Preference.TEMP_CELSIUS.getFrom(prefs))
                  .putBoolean(USE_GRAMS, Account.Preference.WEIGHT_METRIC.getFrom(prefs))
                  .putBoolean(USE_CENTIMETERS, Account.Preference.HEIGHT_METRIC.getFrom(prefs))
                  .putBoolean(USE_24_TIME, Account.Preference.TIME_TWENTY_FOUR_HOUR.getFrom(prefs))
                  .apply();

            logEvent("Pulled preferences");

            return null;
        });
    }

    public void pushAccountPreferences() {
        logEvent("Pushing account preferences");

        Map<Account.Preference, Boolean> preferences = new HashMap<>();
        boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        preferences.put(Account.Preference.TIME_TWENTY_FOUR_HOUR, getUse24Time());
        preferences.put(Account.Preference.TEMP_CELSIUS, getBoolean(USE_CELSIUS, defaultMetric));
        preferences.put(Account.Preference.WEIGHT_METRIC, getBoolean(USE_GRAMS, defaultMetric));
        preferences.put(Account.Preference.HEIGHT_METRIC, getBoolean(USE_CENTIMETERS, defaultMetric));
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

