package is.hello.sense.flows.accountsettings.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.model.Account;
import is.hello.sense.flows.accountsettings.interactors.AccountSettingsInteractorContainer;
import is.hello.sense.flows.accountsettings.ui.views.AccountSettingsView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.SensePresenter;
import is.hello.sense.ui.common.SenseFragment;

public class AccountSettingsPresenter extends SensePresenter<AccountSettingsView, AccountSettingsInteractorContainer> {

    public AccountSettingsPresenter(@NonNull final SenseFragment fragment) {
        super(fragment);
    }

    @Override
    protected AccountSettingsView initializeSenseView(@NonNull final Activity activity) {
        return new AccountSettingsView(activity);
    }

    @Override
    protected AccountSettingsInteractorContainer initializeInteractorContainer(@NonNull final SenseFragment fragment) {
        return new AccountSettingsInteractorContainer(fragment);
    }

    @Override
    public void bindAndSubscribeAll() {
        bindAndSubscribe(getInteractorContainer().accountInteractor.subscriptionSubject,
                         this::bind,
                         Functions.LOG_ERROR);
        bindAndSubscribe(getInteractorContainer().facebookInteractor.subscriptionSubject,
                         this::bind,
                         Functions.LOG_ERROR);
    }

    public void updateAccount() {
        getInteractorContainer().accountInteractor.update();
    }

    public void updateFacebook() {
        getInteractorContainer().facebookInteractor.update();
    }

    public void bind(@NonNull FacebookProfile object) {
        Log.e(getClass().getSimpleName(), "Received facebookprofile");
    }

    public void bind(@NonNull Account object) {
        Log.e(getClass().getSimpleName(), "Received account");
    }

}
