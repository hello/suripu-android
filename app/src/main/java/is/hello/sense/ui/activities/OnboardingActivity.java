package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingGettingStartedFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSleepPillColorFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWelcomeFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWhichPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

public class OnboardingActivity extends InjectionActivity {
    private static final String FRAGMENT_TAG = "OnboardingFragment";

    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        //noinspection ConstantConditions
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            int lastCheckpoint = preferences.getInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE);
            switch (lastCheckpoint) {
                case Constants.ONBOARDING_CHECKPOINT_NONE:
                    showIntroductionFragment();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_ACCOUNT:
                    beginBlockingWork(R.string.dialog_loading_message);
                    bindAndSubscribe(apiService.getAccount(), account -> {
                        finishBlockingWork();
                        showBirthday(account);
                    }, e -> {
                        finishBlockingWork();
                        ErrorDialogFragment.presentError(getFragmentManager(), e);
                    });
                    break;

                case Constants.ONBOARDING_CHECKPOINT_QUESTIONS:
                    showGettingStarted();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Boolean.parseBoolean(getString(R.string.build_debug_enabled))) {
            getMenuInflater().inflate(R.menu.onboarding_debug, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem skipItem = menu.findItem(R.id.action_skip);
        int lastCheckpoint = preferences.getInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE);
        skipItem.setEnabled(lastCheckpoint > Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_debug) {
            startActivity(new Intent(this, DebugActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_skip) {
            showHomeActivity();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            transaction.add(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
            if (addToBackStack)
                transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_title_confirm_leave_onboarding);
            builder.setMessage(R.string.dialog_message_confirm_leave_onboarding);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> super.onBackPressed());
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        } else {
            super.onBackPressed();
        }
    }

    //region Steps

    public void passedCheckPoint(int checkPoint) {
        preferences
                .edit()
                .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, checkPoint)
                .apply();

        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void showIntroductionFragment() {
        showFragment(new OnboardingIntroductionFragment(), true);
    }

    public void showSignIn() {
        showFragment(new OnboardingSignInFragment(), true);
    }

    public void showRegistration() {
        showFragment(new OnboardingRegisterFragment(), true);
    }

    public void showBirthday(@NonNull Account account) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        showFragment(OnboardingRegisterBirthdayFragment.newInstance(account), false);
    }

    public void showGender(@NonNull Account account) {
        showFragment(OnboardingRegisterGenderFragment.newInstance(account), true);
    }

    public void showHeight(@NonNull Account account) {
        showFragment(OnboardingRegisterHeightFragment.newInstance(account), true);
    }

    public void showWeight(@NonNull Account account) {
        showFragment(OnboardingRegisterWeightFragment.newInstance(account), true);
    }

    public void showGettingStarted() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_QUESTIONS);

        showFragment(new OnboardingGettingStartedFragment(), false);
    }

    public void showWhichPill() {
        showFragment(new OnboardingWhichPillFragment(), true);
    }

    public void showSetupSense(boolean secondPill) {
        if (secondPill) {
            Bundle arguments = new Bundle();
            arguments.putBoolean(OnboardingPairSenseFragment.ARG_IS_SECOND_USER, true);
            OnboardingStaticStepFragment.Builder builder = new OnboardingStaticStepFragment.Builder();
            builder.setLayout(R.layout.sub_fragment_onboarding_2nd_user_setup_sense);
            builder.setNextFragmentClass(OnboardingPairSenseFragment.class);
            builder.setNextFragmentArguments(arguments);
            builder.setAnalyticsEvent(Analytics.EVENT_ONBOARDING_ADD_PILL);
            showFragment(builder.build(), true);
        } else {
            OnboardingStaticStepFragment.Builder builder = new OnboardingStaticStepFragment.Builder();
            builder.setLayout(R.layout.sub_fragment_onboarding_1st_user_setup_sense);
            builder.setNextFragmentClass(OnboardingPairSenseFragment.class);
            builder.setAnalyticsEvent(Analytics.EVENT_ONBOARDING_SENSE_SETUP);
            showFragment(builder.build(), true);
        }
    }

    public void showSelectWifiNetwork() {
        showFragment(new OnboardingWifiNetworkFragment(), true);
    }

    public void showSignIntoWifiNetwork(@Nullable ScanResult network) {
        showFragment(OnboardingSignIntoWifiFragment.newInstance(network), true);
    }

    public void showSetupPill() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SENSE);

        OnboardingStaticStepFragment.Builder builder = new OnboardingStaticStepFragment.Builder();
        builder.setLayout(R.layout.sub_fragment_onboarding_pill_intro);
        builder.setNextFragmentClass(OnboardingSleepPillColorFragment.class);
        builder.setAnalyticsEvent(Analytics.EVENT_ONBOARDING_SETUP_PILL);
        showFragment(builder.build(), true);
    }

    public void showPairPill(int selectedColorIndex) {
        showFragment(OnboardingPairPillFragment.newInstance(selectedColorIndex), true);
    }

    public void showWelcome() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_PILL);

        showFragment(new OnboardingWelcomeFragment(), false);
    }

    //endregion


    //region Presenting Blocking Work

    public void beginBlockingWork(@StringRes int titleResId) {
        if (getFragmentManager().findFragmentByTag(LoadingDialogFragment.TAG) != null) {
            LoadingDialogFragment dialogFragment = LoadingDialogFragment.newInstance(getString(titleResId), true);
            dialogFragment.show(getFragmentManager(), LoadingDialogFragment.TAG);
        }
    }

    public void finishBlockingWork() {
        LoadingDialogFragment.close(getFragmentManager());
    }

    //endregion


    public void showHomeActivity() {
        preferences
                .edit()
                .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, true)
                .apply();

        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
