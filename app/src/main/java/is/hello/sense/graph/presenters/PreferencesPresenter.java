package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;

import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

@Singleton public class PreferencesPresenter extends Presenter {
    public static final String UNIT_SYSTEM = "unit_system_name";
    public static final String USE_24_TIME = "use_24_time";

    public static final String PAIRED_DEVICE_TIME_ZONE = "paired_device_time_zone";
    public static final String PAIRED_DEVICE_ADDRESS = "paired_device_address";
    public static final String PAIRED_DEVICE_SSID = "paired_device_ssid";
    public static final String PAIRED_PILL_ID = "paired_pill_id";

    public static final String LAST_ONBOARDING_CHECK_POINT = "last_onboarding_check_point";
    public static final String ONBOARDING_COMPLETED = "onboarding_completed";

    public static final String USE_MODERN_TIMELINE = "use_modern_timeline";


    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final AccountPresenter accountPresenter;

    /**
     * SharedPreferences only keeps a weak reference to its OnSharedPreferenceChangeListener,
     * so we have to keep a strong reference to them somewhere if we want updates.
     */
    private final Set<SharedPreferences.OnSharedPreferenceChangeListener> strongListeners = Collections.synchronizedSet(new HashSet<>());

    private Observable<UnitSystem> cachedUnitSystem;

    public @Inject PreferencesPresenter(@NonNull Context context,
                                        @NonNull @GlobalSharedPreferences SharedPreferences sharedPreferences,
                                        @NonNull AccountPresenter accountPresenter) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.accountPresenter = accountPresenter;

        Observable<Intent> logOut = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOut.subscribe(ignored -> clear(), Functions.LOG_ERROR);
    }

    
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

        return Observable.create(s -> {
            accountPresenter.preferences()
                            .subscribe(prefs -> {
                                Boolean use24Time = (Boolean) prefs.get(AccountPreference.Key.TIME_TWENTY_FOUR_HOUR);
                                Boolean useMetric = (Boolean) prefs.get(AccountPreference.Key.TEMP_CELCIUS);

                                String unitSystemName;
                                if (useMetric != null && useMetric) {
                                    unitSystemName = MetricUnitSystem.NAME;
                                } else {
                                    unitSystemName = UsCustomaryUnitSystem.NAME;
                                }

                                edit().putString(UNIT_SYSTEM, unitSystemName)
                                      .putBoolean(USE_24_TIME, use24Time != null && use24Time)
                                      .apply();

                                logEvent("Pulled preferences");

                                s.onNext(null);
                                s.onCompleted();
                            }, e -> {
                                logEvent("Could not pull preferences from backend. " + e);

                                s.onError(e);
                            });
        });
    }

    public Observable<Void> pushAccountPreferences() {
        logEvent("Pushing account preferences");

        AccountPreference use24Time = new AccountPreference(AccountPreference.Key.TIME_TWENTY_FOUR_HOUR);
        use24Time.setEnabled(getUse24Time());

        AccountPreference useMetric = new AccountPreference(AccountPreference.Key.TEMP_CELCIUS);
        useMetric.setEnabled(MetricUnitSystem.NAME.equals(getString(UNIT_SYSTEM, UsCustomaryUnitSystem.NAME)));

        return Observable.combineLatest(accountPresenter.updatePreference(use24Time),
                                        accountPresenter.updatePreference(useMetric),
                                        (l, r) -> null);
    }

    //endregion


    //region Instantaneous Values

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
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
        return Observable.create(s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key)) {
                    s.onNext(producer.call(key, defaultValue));
                }
            };

            Subscription subscription = Subscriptions.create(() -> {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
                strongListeners.remove(changeListener);
            });
            s.add(subscription);

            sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
            strongListeners.add(changeListener);
            s.onNext(producer.call(key, defaultValue));
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

    public Observable<Long> observableLong(@NonNull String key, long defaultValue) {
        return observableValue(key, defaultValue, sharedPreferences::getLong);
    }

    public Observable<Float> observableFloat(@NonNull String key, float defaultValue) {
        return observableValue(key, defaultValue, sharedPreferences::getFloat);
    }

    //endregion


    //region Wrappers

    public @NonNull DateTimeZone getSenseTimeZone() {
        String pairedDeviceTimeZone = getString(PAIRED_DEVICE_TIME_ZONE, null);
        if (TextUtils.isEmpty(pairedDeviceTimeZone)) {
            return DateTimeZone.getDefault();
        } else {
            return DateTimeZone.forID(pairedDeviceTimeZone);
        }
    }

    public boolean getUse24Time() {
        return getBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    public boolean getUseModernTimeline() {
        return getBoolean(USE_MODERN_TIMELINE, true);
    }

    public Observable<Boolean> observableUse24Time() {
        return observableBoolean(USE_24_TIME, DateFormat.is24HourFormat(context));
    }

    public Observable<UnitSystem> observableUnitSystem() {
        if (cachedUnitSystem == null) {
            String defaultSystemName = UnitSystem.getLocaleUnitSystemName(Locale.getDefault());
            Observable<String> systemName = observableString(UNIT_SYSTEM, defaultSystemName);
            this.cachedUnitSystem = systemName.map(UnitSystem::createUnitSystemWithName);
        }

        return cachedUnitSystem;
    }

    //endregion
}

