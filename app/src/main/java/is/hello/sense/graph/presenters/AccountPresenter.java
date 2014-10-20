package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.util.Logger;
import rx.observables.BlockingObservable;
import rx.subjects.ReplaySubject;

public class AccountPresenter extends Presenter {
    @Inject ApiService apiService;

    public final ReplaySubject<Account> account = ReplaySubject.createWithSize(1);

    @Override
    public @Nullable Parcelable onSaveState() {
        try {
            BlockingObservable<Account> accountObservable = BlockingObservable.from(account);
            Account account = accountObservable.single();
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
    public void update() {
        apiService.getAccount().subscribe(account::onNext, account::onError);
    }


    public void saveAccount(@NonNull Account updatedAccount) {
        apiService.updateAccount(updatedAccount).subscribe(account::onNext, account::onError);
    }
}
