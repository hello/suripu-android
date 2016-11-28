package is.hello.sense.mvvm;

import android.support.annotation.DrawableRes;

import is.hello.sense.R;

public class OnboardingFeatureModel {

    @DrawableRes
    public final int[] DIAGRAMS = {
            R.color.transparent,
            R.drawable.onboarding_intro_feature_alarm,
            R.drawable.onboarding_intro_feature_timeline,
            R.drawable.onboarding_intro_feature_sleep_score,
            R.drawable.onboarding_intro_feature_conditions,
            R.color.transparent,
    };
}
