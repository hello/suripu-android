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

public class OnboardingRegisterWeightFragment extends SenseFragment {
    @Inject
    PreferencesPresenter preferences;

    private ScaleView scale;
    private TextView scaleReading;

    private boolean hasAnimated = false;

    public OnboardingRegisterWeightFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
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
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register_weight, container, false);

        this.scale = (ScaleView) view.findViewById(R.id.fragment_onboarding_register_weight_scale);
        this.scaleReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_weight_scale_reading);

        final boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        final boolean useGrams = preferences.getBoolean(PreferencesPresenter.USE_GRAMS, defaultMetric);
        if (useGrams) {
            scale.setOnValueChangedListener(pounds -> {
                final int kilograms = (int) UnitOperations.poundsToKilograms(pounds);
                scaleReading.setText(getString(R.string.weight_kg_fmt, kilograms));
            });
        } else {
            scale.setOnValueChangedListener(pounds -> scaleReading.setText(getString(R.string.weight_pounds_fmt, pounds)));
        }

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getWeight() != null) {
            final int weightInGrams = account.getWeight();
            final int pounds = (int) UnitOperations.gramsToPounds(weightInGrams);
            scale.setValue(pounds, true);
        }

        final Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        final Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        Views.setSafeOnClickListener(nextButton, ignored -> {
            skipButton.setEnabled(false);
            nextButton.setEnabled(false);
            next();
        });

        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setSafeOnClickListener(skipButton, ignored -> {
                skipButton.setEnabled(false);
                nextButton.setEnabled(false);
                Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "weight"));
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

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (!hasAnimated && account.getWeight() != null) {
            scale.setValue(scale.getMinValue(), true);
            scale.postDelayed(() -> {
                int weightInGrams = account.getWeight();
                int pounds = (int) UnitOperations.gramsToPounds(weightInGrams);
                scale.animateToValue(pounds);
            }, 250);
            this.hasAnimated = true;
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasAnimated", true);
    }


    public void next() {
        final AccountEditor.Container container = AccountEditor.getContainer(this);
        try {
            if (!scale.isAnimating()) {
                final int pounds = scale.getValue();
                final int grams = (int) UnitOperations.poundsToGrams(pounds);
                container.getAccount().setWeight(grams);
            }
        } catch (final NumberFormatException e) {
            Logger.warn(OnboardingRegisterWeightFragment.class.getSimpleName(), "Invalid input fed to weight fragment, ignoring", e);
        } finally {
            container.onAccountUpdated(this);
        }
    }
}
