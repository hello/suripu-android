package is.hello.sense.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingTaskFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWelcomeFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.util.Constants;

public class OnboardingActivity extends SenseActivity {
    private static final String FRAGMENT_TAG = "OnboardingFragment";
    private static final String BLOCKING_WORK_TAG = "BlockingWorkFragment";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            int lastCheckpoint = sharedPreferences.getInt(Constants.GLOBAL_PREF_LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE);
            switch (lastCheckpoint) {
                case Constants.ONBOARDING_CHECKPOINT_NONE:
                    showIntroductionFragment();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_ACCOUNT:
                    showSetupSense();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_SENSE:
                    showSetupPill();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_PILL:
                    showHomeActivity();
                    break;
            }
        }
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


    //region Steps

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
        sharedPreferences
                .edit()
                .putInt(Constants.GLOBAL_PREF_LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_ACCOUNT)
                .apply();

        showFragment(OnboardingStaticStepFragment.newInstance(R.layout.sub_fragment_onboarding_setup_sense, OnboardingPairSenseFragment.class, null));
    }

    public void showSelectWifiNetwork() {
        showFragment(new OnboardingWifiNetworkFragment());
    }

    public void showSignIntoWifiNetwork(@Nullable ScanResult network) {
        showFragment(OnboardingSignIntoWifiFragment.newInstance(network));
    }

    public void showSetupPill() {
        sharedPreferences
                .edit()
                .putInt(Constants.GLOBAL_PREF_LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_SENSE)
                .apply();

        showWelcome();
    }

    public void showWelcome() {
        sharedPreferences
                .edit()
                .putInt(Constants.GLOBAL_PREF_LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_PILL)
                .apply();

        showFragment(new OnboardingWelcomeFragment());
    }

    //endregion


    //region Presenting Blocking Work

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

    //endregion


    public void showHomeActivity() {
        sharedPreferences
                .edit()
                .putBoolean(Constants.GLOBAL_PREF_ONBOARDING_COMPLETED, true)
                .apply();

        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
