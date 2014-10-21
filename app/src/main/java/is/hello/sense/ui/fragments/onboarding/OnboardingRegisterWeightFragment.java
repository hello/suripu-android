package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.common.InjectionFragment;

public class OnboardingRegisterWeightFragment extends InjectionFragment {
    private static final String ARG_ACCOUNT = OnboardingRegisterWeightFragment.class.getName() + ".ARG_ACCOUNT";

    @Inject ApiService apiService;

    private Account account;

    public static OnboardingRegisterWeightFragment newInstance(@NonNull Account account) {
        OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();

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
