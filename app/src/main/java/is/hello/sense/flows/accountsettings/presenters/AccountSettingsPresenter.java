package is.hello.sense.flows.accountsettings.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.flows.accountsettings.interactors.AccountSettingsInteractorContainer;
import is.hello.sense.flows.accountsettings.ui.views.AccountSettingsView;
import is.hello.sense.mvp.presenters.SensePresenter;

public class AccountSettingsPresenter extends SensePresenter<AccountSettingsView, AccountSettingsInteractorContainer> {

    public AccountSettingsPresenter(@NonNull final Activity activity) {
        super(activity);
    }

    @Override
    protected AccountSettingsView initializeSenseView(@NonNull final Activity activity) {
        return new AccountSettingsView(activity);
    }

    @Override
    protected AccountSettingsInteractorContainer initializeInteractorContainer() {
        return new AccountSettingsInteractorContainer();
    }
}
