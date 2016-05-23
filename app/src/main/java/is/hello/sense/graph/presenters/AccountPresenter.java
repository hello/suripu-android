package is.hello.sense.graph.presenters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.regex.Pattern;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import retrofit.mime.TypedFile;
import rx.Observable;

public class AccountPresenter extends ValuePresenter<Account> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^.+@.+\\..+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Inject
    ApiService apiService;
    @Inject
    ApiSessionManager sessionManager;
    @Inject
    PreferencesPresenter preferences;

    public final PresenterSubject<Account> account = this.subject;
    @NonNull
    private final Context context;

    public
    @Inject
    AccountPresenter(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Account> provideUpdateObservable() {
        return apiService.getAccount()
                         .doOnNext(account -> {
                             logEvent("updated account creation date preference");
                             preferences.putLocalDate(PreferencesPresenter.ACCOUNT_CREATION_DATE,
                                                      account.getCreated());
                         });
    }


    //region Validation

    public static
    @NonNull
    String normalizeInput(@Nullable CharSequence value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        } else {
            return value.toString().trim();
        }
    }

    public static boolean validateName(@Nullable CharSequence name) {
        return !TextUtils.isEmpty(name);
    }

    public static boolean validateEmail(@Nullable CharSequence email) {
        return (!TextUtils.isEmpty(email) &&
                EMAIL_PATTERN.matcher(email).matches());
    }

    public static boolean validatePassword(@Nullable CharSequence password) {
        return (!TextUtils.isEmpty(password) &&
                password.length() >= MIN_PASSWORD_LENGTH);
    }

    //endregion


    //region Updates

    public Observable<Account> saveAccount(@NonNull Account updatedAccount) {
        return apiService.updateAccount(updatedAccount)
                         .doOnNext(account::onNext);
    }

    public Observable<Account> updateEmail(@NonNull String email) {
        return latest().flatMap(account -> {
            Account updatedAccount = account.clone();
            updatedAccount.setEmail(email);
            return apiService.updateEmailAddress(updatedAccount)
                             .doOnNext(this.account::onNext);
        });
    }

    public Observable<SenseTimeZone> currentTimeZone() {
        return apiService.currentTimeZone();
    }

    public Observable<SenseTimeZone> updateTimeZone(@NonNull SenseTimeZone senseTimeZone) {
        return apiService.updateTimeZone(senseTimeZone);
    }

    //endregion

    public Observable<MultiDensityImage> updateProfilePicture(@NonNull TypedFile picture){
        return apiService.uploadProfilePhoto(picture)
                .doOnError(Functions.LOG_ERROR);
    }

    //region Preferences

    public Observable<Account.Preferences> preferences() {
        return apiService.accountPreferences();
    }

    public Observable<Account.Preferences> updatePreferences(@NonNull Account.Preferences changes) {
        return apiService.updateAccountPreferences(changes);
    }

    public Observable<Void> pullAccountPreferences() {
        preferences.logEvent("Pulling preferences from backend");

        return preferences().map(prefs -> {
            preferences.edit()
                       .putBoolean(PreferencesPresenter.PUSH_SCORE_ENABLED, prefs.pushScore)
                       .putBoolean(PreferencesPresenter.PUSH_ALERT_CONDITIONS_ENABLED, prefs.pushAlertConditions)
                       .putBoolean(PreferencesPresenter.ENHANCED_AUDIO_ENABLED, prefs.enhancedAudioEnabled)
                       .putBoolean(PreferencesPresenter.USE_CELSIUS, prefs.useCelsius)
                       .putBoolean(PreferencesPresenter.USE_GRAMS, prefs.useMetricWeight)
                       .putBoolean(PreferencesPresenter.USE_CENTIMETERS, prefs.useMetricHeight)
                       .putBoolean(PreferencesPresenter.USE_24_TIME, prefs.use24Time)
                       .apply();

            logEvent("Pulled preferences");

            return null;
        });
    }

    public void pushAccountPreferences() {
        logEvent("Pushing account preferences");

        Account.Preferences update = new Account.Preferences();
        boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        update.pushScore = preferences.getBoolean(PreferencesPresenter.PUSH_SCORE_ENABLED, true);
        update.pushAlertConditions = preferences.getBoolean(PreferencesPresenter.PUSH_ALERT_CONDITIONS_ENABLED, true);
        update.enhancedAudioEnabled = preferences.getBoolean(PreferencesPresenter.ENHANCED_AUDIO_ENABLED, false);
        update.use24Time = preferences.getUse24Time();
        update.useCelsius = preferences.getBoolean(PreferencesPresenter.USE_CELSIUS, defaultMetric);
        update.useMetricWeight = preferences.getBoolean(PreferencesPresenter.USE_GRAMS, defaultMetric);
        update.useMetricHeight = preferences.getBoolean(PreferencesPresenter.USE_CENTIMETERS, defaultMetric);
        updatePreferences(update)
                .subscribe(ignored -> logEvent("Pushed account preferences"),
                           Functions.LOG_ERROR);
    }

    //endregion


    //region Logging out
    public void logOut() {
        sessionManager.logOut();
        NotificationRegistration.resetAppVersion(context);
        Analytics.signOut();
    }

    //endregion
}
