package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.Account;

public class OnboardingRegisterBirthdayFragment extends Fragment {
    private static final String ARG_ACCOUNT = OnboardingRegisterBirthdayFragment.class.getName() + ".ARG_ACCOUNT";

    private Account account;

    public static OnboardingRegisterBirthdayFragment newInstance(@NonNull Account account) {
        OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ACCOUNT, account);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = (Account) getArguments().getSerializable(ARG_ACCOUNT);
    }
}
