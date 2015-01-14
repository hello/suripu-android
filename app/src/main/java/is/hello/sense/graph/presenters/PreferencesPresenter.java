package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
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


    private final SharedPreferences sharedPreferences;

    /**
     * SharedPreferences only keeps a weak reference to its OnSharedPreferenceChangeListener,
     * so we have to keep a strong reference to them somewhere if we want updates.
     */
    private final Set<SharedPreferences.OnSharedPreferenceChangeListener> strongListeners = Collections.synchronizedSet(new HashSet<>());

    public @Inject PreferencesPresenter(@NonNull Context context, @NonNull @GlobalSharedPreferences SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

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
        return Observable.create((Observable.OnSubscribe<T>) s -> {
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
}
