package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.app.FragmentTransaction;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment;

public class OnboardingActivity extends InjectionActivity {
    private static final String FRAGMENT_TAG = "OnboardingFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (savedInstanceState == null)
            showFragment(new OnboardingPairSenseFragment());
    }


    public void showFragment(@NonNull Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            transaction.add(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }


    public void showIntroductionFragment() {
        showFragment(new OnboardingIntroductionFragment());
    }

    public void showRegistration() {
        showFragment(new OnboardingRegisterFragment());
    }

    public void showSignIn() {
        showFragment(new OnboardingSignInFragment());
    }

    public void showSetupSense() {
        showFragment(OnboardingStaticStepFragment.newInstance(R.layout.sub_fragment_onboarding_setup_sense, OnboardingPairSenseFragment.class, null));
    }

    public void showHomeActivity() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
