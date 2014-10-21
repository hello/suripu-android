package is.hello.sense.ui.activities;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingTaskFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;

public class OnboardingActivity extends InjectionActivity {
    private static final String FRAGMENT_TAG = "OnboardingFragment";
    private static final String BLOCKING_WORK_TAG = "BlockingWorkFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (savedInstanceState == null)
            showSelectWifiNetwork();
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

    public void showSelectWifiNetwork() {
        showFragment(new OnboardingWifiNetworkFragment());
    }

    public void showSignIntoWifiNetwork(@Nullable ScanResult network) {
        showFragment(OnboardingSignIntoWifiFragment.newInstance(network));
    }

    public void beginBlockingWork(@StringRes int titleResId) {
        if (getFragmentManager().findFragmentByTag(BLOCKING_WORK_TAG) != null)
            return;

        OnboardingTaskFragment fragment = OnboardingTaskFragment.newInstance(titleResId);
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.activity_onboarding_container, fragment, BLOCKING_WORK_TAG)
                .commit();
    }

    public void finishBlockingWork() {
        Fragment fragment = getFragmentManager().findFragmentByTag(BLOCKING_WORK_TAG);
        if (fragment == null)
            return;

        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .remove(fragment)
                .commit();
    }


    public void showHomeActivity() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
