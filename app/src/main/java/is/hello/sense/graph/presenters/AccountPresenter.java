package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.HashMap;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class AccountPresenter extends ValuePresenter<Account> {
    @Inject ApiService apiService;

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


    public Observable<Account> saveAccount(@NonNull Account updatedAccount) {
        return apiService.updateAccount(updatedAccount)
                         .doOnNext(account::onNext);
    }

    public Observable<Account> updateEmail(@NonNull String email) {
        return account.take(1).flatMap(account -> {
            Account updatedAccount = account.clone();
            updatedAccount.setEmail(email);
            return apiService.updateEmailAddress(updatedAccount)
                             .doOnNext(this.account::onNext);
        });
    }

    public Observable<SenseTimeZone> updateTimeZone(@NonNull SenseTimeZone senseTimeZone) {
        return apiService.updateTimeZone(senseTimeZone);
    }

    public Observable<HashMap<AccountPreference.Key, Object>> preferences() {
        return apiService.accountPreferences();
    }

    public Observable<AccountPreference> updatePreference(@NonNull AccountPreference update) {
        return apiService.updateAccountPreference(update);
    }
}
