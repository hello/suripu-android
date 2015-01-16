package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.ScaleView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingRegisterHeightFragment extends AccountEditingFragment implements ScaleView.OnValueChangedListener {
    private Account account;

    private ScaleView scale;
    private TextView scaleReading;
    private TextView secondaryReading;


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

        this.scale = (ScaleView) view.findViewById(R.id.fragment_onboarding_register_height_scale);
        this.scaleReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_height_scale_reading);
        this.secondaryReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_height_scale_reading_secondary);

        scale.setOnValueChangedListener(this);

        if (account.getHeight() != null) {
            int height = account.getHeight();
            scale.setValueAsync(height);
            onValueChanged(height);
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


    @Override
    public void onValueChanged(int centimeters) {
        long totalInches = UnitOperations.centimetersToInches(centimeters);
        long feet = totalInches / 12;
        long inches = totalInches % 12;
        scaleReading.setText(getString(R.string.height_inches_fmt, feet, inches));
        secondaryReading.setText(getString(R.string.height_cm_fmt, centimeters));
    }


    public void next() {
        try {
            account.setHeight(scale.getValue());
            getContainer().onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterHeightFragment.class.getSimpleName(), "Invalid input fed to height fragment, ignoring.", e);
        }
    }
}
