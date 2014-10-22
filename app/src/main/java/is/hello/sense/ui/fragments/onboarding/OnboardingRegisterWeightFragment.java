package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

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


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_weight, container, false);

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(this::next);

        return view;
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


    public void next(@NonNull View sender) {
        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.beginBlockingWork(R.string.dialog_loading_message);
        bindAndSubscribe(apiService.updateAccount(account), ignored -> {
            activity.finishBlockingWork();
            activity.showSetupSense();
        }, e -> {
            activity.finishBlockingWork();
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        });
    }
}
