package is.hello.sense.flows.accountsettings.ui.fragments;


import android.os.Bundle;
import android.view.View;

import is.hello.sense.flows.accountsettings.presenters.AccountSettingsPresenter;
import is.hello.sense.mvp.fragments.PresenterFragment;

public class AccountSettingsFragment extends PresenterFragment<AccountSettingsPresenter> {

    @Override
    public AccountSettingsPresenter initializeSensePresenter() {
        return new AccountSettingsPresenter(this);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getSensePresenter().bindAndSubscribeAll();
        getSensePresenter().updateAccount();
    }
}
