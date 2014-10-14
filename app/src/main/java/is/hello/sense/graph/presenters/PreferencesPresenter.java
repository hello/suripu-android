package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

@Singleton public class PreferencesPresenter extends Presenter {
    private final SharedPreferences sharedPreferences;

    /**
     * SharedPreferences only keeps a weak reference to its OnSharedPreferenceChangeListener,
     * so we have to keep a strong reference to them somewhere if we want updates.
     */
    private final Set<SharedPreferences.OnSharedPreferenceChangeListener> strongListeners = Collections.synchronizedSet(new HashSet<>());

    public @Inject PreferencesPresenter(@NonNull Context applicationContext) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    @Override
    public void update() {}


    public @NonNull SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public SharedPreferences.Editor edit() {
        return getSharedPreferences().edit();
    }


    //region Observable Values

    private <T> Observable<T> observableValue(@NonNull String key, @Nullable T defaultValue, Func2<String, T, T> producer) {
        return Observable.create((Observable.OnSubscribe<T>) s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key))
                    s.onNext(producer.call(key, defaultValue));
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
