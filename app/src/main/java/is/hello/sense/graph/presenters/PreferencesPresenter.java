package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@Singleton public class PreferencesPresenter extends Presenter {
    private final SharedPreferences preferences;

    public @Inject PreferencesPresenter(@NonNull Context applicationContext) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    @Override
    public void update() {}


    public Observable<String> observableString(@NonNull String key, @Nullable String defaultValue) {
        return Observable.create((Observable.OnSubscribe<String>) s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key))
                    s.onNext(preferences.getString(key, defaultValue));
            };

            Subscription subscription = Subscriptions.create(() -> preferences.unregisterOnSharedPreferenceChangeListener(changeListener));
            s.add(subscription);

            preferences.registerOnSharedPreferenceChangeListener(changeListener);
            s.onNext(preferences.getString(key, defaultValue));
        });
    }

    public Observable<Boolean> observableBoolean(@NonNull String key, boolean defaultValue) {
        return Observable.create((Observable.OnSubscribe<Boolean>) s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key))
                    s.onNext(preferences.getBoolean(key, defaultValue));
            };

            Subscription subscription = Subscriptions.create(() -> preferences.unregisterOnSharedPreferenceChangeListener(changeListener));
            s.add(subscription);

            preferences.registerOnSharedPreferenceChangeListener(changeListener);
            s.onNext(preferences.getBoolean(key, defaultValue));
        });
    }
}
