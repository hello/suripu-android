package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

public class BasePreferencesPresenter extends Presenter {

    /**
     * SharedPreferences only keeps a weak reference to its OnSharedPreferenceChangeListener,
     * so we have to keep a strong reference to them somewhere if we want updates.
     */
    private final Set<SharedPreferences.OnSharedPreferenceChangeListener> strongListeners = Collections.synchronizedSet(new HashSet<>());

    protected final Context context;
    private final SharedPreferences sharedPreferences;

    public BasePreferencesPresenter(@NonNull Context context, @NonNull SharedPreferences sharedPreferences){
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    //region Editing

    public SharedPreferences.Editor edit() {
        return sharedPreferences.edit();
    }

    public void putLocalDate(@NonNull String key, @Nullable LocalDate localDate) {
        final SharedPreferences.Editor editor = edit();
        if (localDate != null) {
            editor.putString(key, localDate.toString(ISODateTimeFormat.date()));
        } else {
            editor.remove(key);
        }
        editor.apply();
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

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public long getLong(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public LocalDate getLocalDate(String key) {
        final String timestamp = sharedPreferences.getString(key, null);
        if (TextUtils.isEmpty(timestamp)) {
            return null;
        } else {
            return LocalDate.parse(timestamp, ISODateTimeFormat.date());
        }
    }

    //endregion


    //region Observable Values

    private <T> Observable<T> observableValue(@NonNull String key, @Nullable T defaultValue, Func2<String, T, T> producer) {
        return Observable.create(subscriber -> {
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
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
            SharedPreferences.OnSharedPreferenceChangeListener changeListener = (prefs, changedKey) -> {
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
}
