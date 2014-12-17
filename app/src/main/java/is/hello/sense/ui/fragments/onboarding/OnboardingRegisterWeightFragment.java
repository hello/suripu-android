package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;

public class OnboardingRegisterWeightFragment extends AccountEditingFragment {
    private Account account;
    private EditText weightText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_WEIGHT, null);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_weight, container, false);

        this.weightText = (EditText) view.findViewById(R.id.fragment_onboarding_register_weight);
        weightText.setOnEditorActionListener(new EditorActionHandler(this::next) {
            @Override
            protected boolean isValid(@Nullable CharSequence value) {
                try {
                    return (value != null && Integer.parseInt(value.toString()) > 0);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });

        if (account.getWeight() != null) {
            long weight = UnitOperations.gramsToPounds(account.getWeight());
            weightText.setText(Long.toString(weight));
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> next());

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (getWantsSkipButton()) {
            skipButton.setOnClickListener(ignored -> getContainer().onAccountUpdated(this));
        } else {
            skipButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }


    public void next() {
        try {
            int weight = Integer.parseInt(weightText.getText().toString());
            account.setWeight(UnitOperations.poundsToGrams(weight));
            getContainer().onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterWeightFragment.class.getSimpleName(), "Invalid input fed to weight fragment, ignoring", e);
        }
    }
}
