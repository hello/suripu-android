package is.hello.sense.flows.accountsettings.ui.fragments;

import is.hello.sense.flows.accountsettings.presenters.AccountSettingsPresenter;
import is.hello.sense.mvp.fragments.PresenterFragment;

public class AccountSettingsFragment extends PresenterFragment<AccountSettingsPresenter> {
    @Override
    public AccountSettingsPresenter initializeSensePresenter() {
        return new AccountSettingsPresenter(getActivity());
    }
    //todo support old functions
}
