package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Gender;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterGenderFragment extends SenseFragment
        implements SelectorView.OnSelectionChangedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_GENDER, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register_gender, container, false);

        final SelectorView genderSelector = (SelectorView) view.findViewById(R.id.fragment_onboarding_register_gender_mode);
        genderSelector.setOnSelectionChangedListener(this);

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getGender() != null) {
            if (account.getGender() == Gender.OTHER) {
                genderSelector.setSelectedIndex(SelectorView.EMPTY_SELECTION);
            } else {
                genderSelector.setSelectedIndex(account.getGender().ordinal());
            }
        }

        final Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> {
            AccountEditor.getContainer(this).onAccountUpdated(this);
        });

        final Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setSafeOnClickListener(skipButton, ignored -> {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "gender"));

                account.setGender(Gender.OTHER);
                AccountEditor.getContainer(this).onAccountUpdated(this);
            });
        } else {
            skipButton.setVisibility(View.GONE);
        }

        return view;
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setGender(Gender.values()[newSelectionIndex]);
    }
}
