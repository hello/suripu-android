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
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.ScaleView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingRegisterWeightFragment extends AccountEditingFragment {
    @Inject PreferencesPresenter preferences;

    private ScaleView scale;
    private TextView scaleReading;

    private boolean hasAnimated = false;

    public OnboardingRegisterWeightFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasAnimated = savedInstanceState.getBoolean("hasAnimated", false);
        }

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_WEIGHT, null);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_weight, container, false);

        this.scale = (ScaleView) view.findViewById(R.id.fragment_onboarding_register_weight_scale);
        this.scaleReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_weight_scale_reading);

        boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        boolean useGrams = preferences.getBoolean(PreferencesPresenter.USE_GRAMS, defaultMetric);
        if (useGrams) {
            scale.setOnValueChangedListener(pounds -> {
                int kilograms = (int) UnitOperations.poundsToKilograms(pounds);
                scaleReading.setText(getString(R.string.weight_kg_fmt, kilograms));
            });
        } else {
            scale.setOnValueChangedListener(pounds -> {
                scaleReading.setText(getString(R.string.weight_pounds_fmt, pounds));
            });
        }

        Account account = getContainer().getAccount();
        if (account.getWeight() != null) {
            int weightInGrams = Math.round(account.getWeight());
            int pounds = (int) UnitOperations.gramsToPounds(weightInGrams);
            scale.setValue(pounds, true);
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> next());

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (getWantsSkipButton()) {
            Views.setSafeOnClickListener(skipButton, ignored -> {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "weight"));
                getContainer().onAccountUpdated(this);
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

        Account account = getContainer().getAccount();
        if (!hasAnimated && account.getWeight() != null) {
            scale.setValue(scale.getMinValue(), true);
            scale.postDelayed(() -> {
                int weightInGrams = Math.round(account.getWeight());
                int pounds = (int) UnitOperations.gramsToPounds(weightInGrams);
                scale.animateToValue(pounds);
            }, 250);
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
            if (!scale.isAnimating()) {
                int pounds = scale.getValue();
                int grams = (int) UnitOperations.poundsToGrams(pounds);
                getContainer().getAccount().setWeight(grams);
            }
            getContainer().onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterWeightFragment.class.getSimpleName(), "Invalid input fed to weight fragment, ignoring", e);
        }
    }
}
