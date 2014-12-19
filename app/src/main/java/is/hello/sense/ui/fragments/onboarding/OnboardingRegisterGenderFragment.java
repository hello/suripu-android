package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
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
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterGenderFragment extends AccountEditingFragment implements SelectorLinearLayout.OnSelectionChangedListener, SelectorLinearLayout.ButtonStyler {
    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_GENDER, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_gender, container, false);

        SelectorLinearLayout genderMode = (SelectorLinearLayout) view.findViewById(R.id.fragment_onboarding_register_gender_mode);
        genderMode.setButtonStyler(this);
        genderMode.setOnSelectionChangedListener(this);
        if (account.getGender() != null) {
            if (account.getGender() == Gender.OTHER)
                genderMode.setSelectedIndex(SelectorLinearLayout.EMPTY_SELECTION);
            else
                genderMode.setSelectedIndex(account.getGender().ordinal());
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> getContainer().onAccountUpdated(this));

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (getWantsSkipButton()) {
            Views.setSafeOnClickListener(skipButton, ignored -> {
                account.setGender(Gender.OTHER);
                getContainer().onAccountUpdated(this);
            });
        } else {
            skipButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        account.setGender(Gender.values()[newSelectionIndex]);
    }

    @Override
    public void styleButton(ToggleButton button, boolean checked) {
        if (checked) {
            button.setAlpha(1f);
        } else {
            button.setAlpha(0.4f);
        }
    }
}
