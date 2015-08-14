package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.regex.Pattern;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class AccountPresenter extends ValuePresenter<Account> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^.+@.+\\..+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;

    public final PresenterSubject<Account> account = this.subject;


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
        return apiService.getAccount();
    }


    //region Validation

    public static @NonNull String normalizeInput(@Nullable CharSequence value) {
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


    //region Preferences

    public Observable<Account.Preferences> preferences() {
        return apiService.accountPreferences();
    }

    public Observable<Account.Preferences> updatePreferences(@NonNull Account.Preferences changes) {
        return apiService.updateAccountPreferences(changes);
    }

    //endregion


    //region Logging out

    public void logOut() {
        sessionManager.logOut();
    }

    //endregion
}
