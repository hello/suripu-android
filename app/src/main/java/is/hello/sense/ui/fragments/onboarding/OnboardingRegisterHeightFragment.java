package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.ScaleView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingRegisterHeightFragment extends SenseFragment {
    @Inject PreferencesPresenter preferences;

    private ScaleView scale;
    private TextView scaleReading;

    private boolean hasAnimated = false;

    public OnboardingRegisterHeightFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasAnimated = savedInstanceState.getBoolean("hasAnimated", false);
        }

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_HEIGHT, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_height, container, false);

        this.scale = (ScaleView) view.findViewById(R.id.fragment_onboarding_register_height_scale);
        this.scaleReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_height_scale_reading);

        boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        boolean useCentimeters = preferences.getBoolean(PreferencesPresenter.USE_CENTIMETERS, defaultMetric);
        if (useCentimeters) {
            scale.setOnValueChangedListener(centimeters -> {
                scaleReading.setText(getString(R.string.height_cm_fmt, centimeters));
            });
        } else {
            scale.setOnValueChangedListener(centimeters -> {
                long totalInches = UnitOperations.centimetersToInches(centimeters);
                long feet = totalInches / 12;
                long inches = totalInches % 12;
                scaleReading.setText(getString(R.string.height_inches_fmt, feet, inches));
            });
        }

        Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getHeight() != null) {
            scale.setValue(account.getHeight(), true);
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> next());

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setSafeOnClickListener(skipButton, ignored -> {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "height"));
                AccountEditor.getContainer(this).onAccountUpdated(this);
            });
        } else {
            skipButton.setVisibility(View.INVISIBLE);
            nextButton.setText(R.string.action_done);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Account account = AccountEditor.getContainer(this).getAccount();
        if (!hasAnimated && account.getHeight() != null) {
            scale.setValue(scale.getMinValue(), true);
            scale.postDelayed(() -> scale.animateToValue(account.getHeight()), 250);
            this.hasAnimated = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasAnimated", true);
    }


    public void next() {
        try {
            final AccountEditor.Container container = AccountEditor.getContainer(this);
            if (!scale.isAnimating()) {
                container.getAccount().setHeight(scale.getValue());
            }
            container.onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterHeightFragment.class.getSimpleName(), "Invalid input fed to height fragment, ignoring.", e);
        }
    }
}
