package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_height, container, false);

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showWeight(account));

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ACCOUNT, account);
    }
}
