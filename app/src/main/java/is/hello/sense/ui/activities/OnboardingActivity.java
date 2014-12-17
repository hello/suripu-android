package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.Onboarding2ndPillInfoFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingDoneFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSetup2ndPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSmartAlarmFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingActivity extends InjectionActivity implements FragmentNavigation, AccountEditingFragment.Container {
    private static final String FRAGMENT_TAG = "OnboardingFragment";

    public static final String EXTRA_START_CHECKPOINT = OnboardingActivity.class.getName() + ".EXTRA_START_CHECKPOINT";
    public static final String EXTRA_WIFI_CHANGE_ONLY = OnboardingActivity.class.getName() + ".EXTRA_WIFI_CHANGE_ONLY";
    public static final String EXTRA_PAIR_ONLY = OnboardingActivity.class.getName() + ".EXTRA_PAIR_ONLY";

    @Inject ApiService apiService;
    @Inject HardwarePresenter hardwarePresenter;
    @Inject PreferencesPresenter preferences;
    private BluetoothAdapter bluetoothAdapter;

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        //noinspection ConstantConditions
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            if (getIntent().getBooleanExtra(EXTRA_WIFI_CHANGE_ONLY, false)) {
                showSelectWifiNetwork(false);
                return;
            }

            int lastCheckpoint = getLastCheckPoint();
            switch (lastCheckpoint) {
                case Constants.ONBOARDING_CHECKPOINT_NONE:
                    showIntroductionFragment();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_ACCOUNT:
                    LoadingDialogFragment.show(getFragmentManager(), getString(R.string.dialog_loading_message), true);
                    bindAndSubscribe(apiService.getAccount(), account -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        showBirthday(account);
                    }, e -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        ErrorDialogFragment.presentError(getFragmentManager(), e);
                    });
                    break;

                case Constants.ONBOARDING_CHECKPOINT_QUESTIONS:
                    showSetupSense();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_SENSE:
                    showPairPill();
                    break;

                case Constants.ONBOARDING_CHECKPOINT_PILL:
                    showSenseColorsInfo();
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

    @Override
    public void showFragment(@NonNull Fragment fragment, @Nullable String title, boolean wantsBackStackEntry) {
        if (!wantsBackStackEntry) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            transaction.add(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            SenseAlertDialog builder = new SenseAlertDialog(this);
            builder.setTitle(R.string.dialog_title_confirm_leave_onboarding);
            builder.setMessage(R.string.dialog_message_confirm_leave_onboarding);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> super.onBackPressed());
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    //region Steps

    public int getLastCheckPoint() {
        if (getIntent().hasExtra(EXTRA_START_CHECKPOINT)) {
            return getIntent().getIntExtra(EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_NONE);
        } else {
            return preferences.getInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE);
        }
    }

    public void passedCheckPoint(int checkPoint) {
        preferences
                .edit()
                .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, checkPoint)
                .apply();

        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void showIntroductionFragment() {
        showFragment(new OnboardingIntroductionFragment(), null, false);
    }

    public void showSignIn() {
        showFragment(new OnboardingSignInFragment(), null, true);
    }

    public void showRegistration() {
        showFragment(new OnboardingRegisterFragment(), null, true);
    }

    public void showBirthday(@NonNull Account account) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        this.account = account;
        showFragment(new OnboardingRegisterBirthdayFragment(), null, false);
    }

    @NonNull
    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void onAccountUpdated(@NonNull AccountEditingFragment updatedBy) {
        if (updatedBy instanceof OnboardingRegisterBirthdayFragment) {
            showFragment(new OnboardingRegisterGenderFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterGenderFragment) {
            showFragment(new OnboardingRegisterHeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterHeightFragment) {
            showFragment(new OnboardingRegisterWeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterWeightFragment) {
            LoadingDialogFragment.show(getFragmentManager(), getString(R.string.dialog_loading_message), true);
            bindAndSubscribe(apiService.updateAccount(account), ignored -> {
                Analytics.trackUserSignUp(account.getAccountId(), account.getName(), DateTime.now());

                LoadingDialogFragment.close(getFragmentManager());
                showSetupSense();
            }, e -> {
                LoadingDialogFragment.close(getFragmentManager());
                ErrorDialogFragment.presentError(getFragmentManager(), e);
            });
        }
    }

    public void showSetupSense() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_QUESTIONS);

        if (bluetoothAdapter.isEnabled()) {
            OnboardingStaticStepFragment.Builder builder = new OnboardingStaticStepFragment.Builder();
            builder.setLayout(R.layout.sub_fragment_onboarding_intro_setup_sense);
            builder.setNextFragmentClass(OnboardingPairSenseFragment.class);
            builder.setAnalyticsEvent(Analytics.EVENT_ONBOARDING_SENSE_SETUP);
            showFragment(builder.build(), null, false);
        } else {
            showFragment(new OnboardingBluetoothFragment(), null, false);
        }
    }

    public void showSelectWifiNetwork(boolean wantsBackStackEntry) {
        showFragment(new OnboardingWifiNetworkFragment(), null, wantsBackStackEntry);
    }

    public void showSignIntoWifiNetwork(@Nullable MorpheusBle.wifi_endpoint network) {
        showFragment(OnboardingSignIntoWifiFragment.newInstance(network), null, true);
    }

    public void showPairPill() {
        if (getIntent().getBooleanExtra(EXTRA_WIFI_CHANGE_ONLY, false)) {
            finish();
            return;
        }

        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SENSE);

        showFragment(new OnboardingPairPillFragment(), null, false);
    }

    public void showPillInstructions() {
        OnboardingStaticStepFragment.Builder builder = new OnboardingStaticStepFragment.Builder();
        builder.setLayout(R.layout.sub_fragment_onboarding_pill_intro);
        builder.setNextFragmentClass(OnboardingSetup2ndPillFragment.class);
        showFragment(builder.build(), null, true);
    }

    public void show2ndPillPairing() {
        showFragment(new Onboarding2ndPillInfoFragment(), null, true);
    }

    public void showSenseColorsInfo() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_PILL);

        OnboardingStaticStepFragment.Builder senseColorsBuilder = new OnboardingStaticStepFragment.Builder();
        senseColorsBuilder.setLayout(R.layout.sub_fragment_onboarding_sense_colors);
        senseColorsBuilder.setHideHelp(true);
        senseColorsBuilder.setNextWantsBackStackEntry(false);

        OnboardingStaticStepFragment.Builder introBuilder = new OnboardingStaticStepFragment.Builder();
        introBuilder.setNextFragmentClass(OnboardingRoomCheckFragment.class);
        introBuilder.setLayout(R.layout.sub_fragment_onboarding_room_check_intro);
        introBuilder.setHideHelp(true);
        introBuilder.setExitAnimationName(ANIMATION_ROOM_CHECK);
        introBuilder.setNextWantsBackStackEntry(false);
        senseColorsBuilder.setNextFragmentArguments(introBuilder.arguments);
        senseColorsBuilder.setNextFragmentClass(OnboardingStaticStepFragment.class);

        showFragment(senseColorsBuilder.build(), null, false);
    }

    public void showSmartAlarmInfo() {
        showFragment(new OnboardingSmartAlarmFragment(), null, false);
    }

    public void showDone() {
        showFragment(new OnboardingDoneFragment(), null, false);
    }

    //endregion


    public void showHomeActivity() {
        preferences
                .edit()
                .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, true)
                .apply();

        hardwarePresenter.clearPeripheral();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_SHOW_UNDERSIDE, true);
        startActivity(intent);
        finish();
    }


    //region Static Step Animation

    public static final String ANIMATION_ROOM_CHECK = "room_check";

    public @Nullable OnboardingStaticStepFragment.ExitAnimationProvider getExitAnimationProviderNamed(@NonNull String name) {
        switch (name) {
            case ANIMATION_ROOM_CHECK: {
                return (container, onCompletion) -> {
                    float endDelta = getResources().getDimensionPixelSize(R.dimen.gap_outer);

                    View continueButton = container.findViewById(R.id.fragment_onboarding_step_continue);
                    animate(continueButton)
                            .slideAndFade(0f, endDelta, 1f, 0f)
                            .addOnAnimationCompleted(finished -> onCompletion.run())
                            .start();

                    ViewGroup introContainer = (ViewGroup) container.findViewById(R.id.sub_fragment_onboarding_room_check_intro_container);
                    for (View child : Views.children(introContainer)) {
                        animate(child)
                                .slideAndFade(0f, -endDelta, 1f, 0f)
                                .start();
                    }
                };
            }

            default: {
                return null;
            }
        }
    }

    //endregion
}
