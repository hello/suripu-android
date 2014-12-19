package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.Views;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;

public class OnboardingRegisterHeightFragment extends AccountEditingFragment {
    private Account account;
    private EditText feetText;
    private EditText inchesText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_HEIGHT, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_height, container, false);

        this.feetText = (EditText) view.findViewById(R.id.fragment_onboarding_register_height_feet);
        this.inchesText = (EditText) view.findViewById(R.id.fragment_onboarding_register_height_inches);
        inchesText.setOnEditorActionListener(new EditorActionHandler(this::next) {
            @Override
            protected boolean isValid(@Nullable CharSequence value) {
                try {
                    return (Integer.parseInt(feetText.getText().toString()) > 0);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });

        if (account.getHeight() != null) {
            long totalInches = UnitOperations.centimetersToInches(account.getHeight());
            long feet = totalInches / 12;
            long inches = totalInches % 12;
            feetText.setText(Long.toString(feet));
            inchesText.setText(Long.toString(inches));
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> next());

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (getWantsSkipButton()) {
            Views.setSafeOnClickListener(skipButton, ignored -> getContainer().onAccountUpdated(this));
        } else {
            skipButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }


    public void next() {
        try {
            int feet = Integer.parseInt(feetText.getText().toString());
            int inches = TextUtils.isEmpty(inchesText.getText()) ? 0 : Integer.parseInt(inchesText.getText().toString());
            int totalInches = (feet * 12) + inches;
            account.setHeight(UnitOperations.inchesToCentimeters(totalInches));
            getContainer().onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterHeightFragment.class.getSimpleName(), "Invalid input fed to height fragment, ignoring.", e);
        }
    }
}
