package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.observables.BlockingObservable;

public class AccountPresenter extends UpdatablePresenter<Account> {
    @Inject ApiService apiService;

    public final PresenterSubject<Account> account = this.subject;

    @Override
    public @Nullable Parcelable onSaveState() {
        try {
            BlockingObservable<Account> accountObservable = BlockingObservable.from(account);
            Account account = accountObservable.first();
            Bundle savedState = new Bundle();
            savedState.putSerializable("account", account);
            return savedState;
        } catch (Exception e) {
            Logger.error(AccountPresenter.class.getSimpleName(), "Could not resolve account for onSaveState, ignoring.", e);
            return null;
        }
    }

    @Override
    public void onRestoreState(@NonNull Parcelable savedState) {
        super.onRestoreState(savedState);

        if (savedState instanceof Bundle) {
            Account account = (Account) ((Bundle) savedState).getSerializable("account");
            if (account != null)
                this.account.onNext(account);
        }
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
        return apiService.getAccount();
    }


    public void saveAccount(@NonNull Account updatedAccount) {
        apiService.updateAccount(updatedAccount).subscribe(account);
    }

    public Observable<SenseTimeZone> updateTimeZone(@NonNull SenseTimeZone senseTimeZone) {
        return apiService.updateTimeZone(senseTimeZone);
    }
}
