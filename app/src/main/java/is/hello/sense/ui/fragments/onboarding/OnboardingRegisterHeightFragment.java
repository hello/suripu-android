package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.Account;

public class OnboardingRegisterHeightFragment extends Fragment {
    private static final String ARG_ACCOUNT = OnboardingRegisterHeightFragment.class.getName() + ".ARG_ACCOUNT";

    private Account account;

    public static OnboardingRegisterHeightFragment newInstance(@NonNull Account account) {
        OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ACCOUNT, account);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.account = (Account) savedInstanceState.getSerializable(ARG_ACCOUNT);
        } else {
            this.account = (Account) getArguments().getSerializable(ARG_ACCOUNT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ACCOUNT, account);
    }
}
