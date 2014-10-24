package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Gender;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterGenderFragment extends Fragment implements SelectorLinearLayout.OnSelectionChangedListener, SelectorLinearLayout.ButtonStyler {
    private static final String ARG_ACCOUNT = OnboardingRegisterGenderFragment.class.getName() + ".ARG_ACCOUNT";

    private Account account;

    public static OnboardingRegisterGenderFragment newInstance(@NonNull Account account) {
        OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();

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
            Analytics.event(Analytics.EVENT_ONBOARDING_GENDER, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_gender, container, false);

        SelectorLinearLayout genderMode = (SelectorLinearLayout) view.findViewById(R.id.fragment_onboarding_register_gender_mode);
        genderMode.setButtonStyler(this);
        genderMode.setOnSelectionChangedListener(this);

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showHeight(account));

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ACCOUNT, account);
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        if (newSelectionIndex == 0) {
            account.setGender(Gender.FEMALE);
        } else if (newSelectionIndex == 1) {
            account.setGender(Gender.MALE);
        }
    }

    @Override
    public void styleButton(ToggleButton button, boolean checked) {
        button.setChecked(checked);
        if (checked)
            button.setAlpha(1f);
        else
            button.setAlpha(0.4f);
    }
}
