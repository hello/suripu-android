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
    private final SharedPreferences sharedPreferences;

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

    public Observable<String> observableString(@NonNull String key, @Nullable String defaultValue) {
        return Observable.create((Observable.OnSubscribe<String>) s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key))
                    s.onNext(sharedPreferences.getString(key, defaultValue));
            };

            Subscription subscription = Subscriptions.create(() -> sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener));
            s.add(subscription);

            sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
            s.onNext(sharedPreferences.getString(key, defaultValue));
        });
    }

    public Observable<Boolean> observableBoolean(@NonNull String key, boolean defaultValue) {
        return Observable.create((Observable.OnSubscribe<Boolean>) s -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
                if (changedKey.equals(key))
                    s.onNext(sharedPreferences.getBoolean(key, defaultValue));
            };

            Subscription subscription = Subscriptions.create(() -> sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener));
            s.add(subscription);

            sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener);
            s.onNext(sharedPreferences.getBoolean(key, defaultValue));
        });
    }
}
