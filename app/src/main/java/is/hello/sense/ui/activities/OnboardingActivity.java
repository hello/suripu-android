package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.squareup.seismic.ShakeDetector;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.Onboarding2ndPillInfoFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingDoneFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingIntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterLocationFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSetup2ndPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSmartAlarmFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.RateLimitingShakeListener;

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

    private @Nullable SensorManager sensorManager;
    private @Nullable ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            this.shakeDetector = new ShakeDetector(new RateLimitingShakeListener(this::showDebugOptions));
        }

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
                    showSetupSense(false);
                    break;

                case Constants.ONBOARDING_CHECKPOINT_SENSE:
                    showPairPill(!getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false));
                    break;

                case Constants.ONBOARDING_CHECKPOINT_PILL:
                    showSenseColorsInfo();
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shakeDetector != null && sensorManager != null) {
            shakeDetector.start(sensorManager);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (shakeDetector != null) {
            shakeDetector.stop();
        }
    }


    @Override
    public void pushFragment(@NonNull Fragment fragment, @Nullable String title, boolean wantsBackStackEntry) {
        if (!wantsBackStackEntry) {
            getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
        getFragmentManager().executePendingTransactions();
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    public void onBackPressed() {
        Fragment topFragment = getTopFragment();
        if (topFragment instanceof BackInterceptingFragment) {
            if (((BackInterceptingFragment) topFragment).onInterceptBack(this::back)) {
                return;
            }
        }

        back();
    }

    public void back() {
        boolean hasStartCheckPoint = getIntent().hasExtra(EXTRA_START_CHECKPOINT);
        boolean wifiChangeOnly = getIntent().getBooleanExtra(EXTRA_WIFI_CHANGE_ONLY, false);
        boolean pairOnly = getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false);
        boolean wantsDialog = (!hasStartCheckPoint && !wifiChangeOnly && !pairOnly);
        if (wantsDialog && getFragmentManager().getBackStackEntryCount() == 0) {
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


    //region Debug Options

    public void showDebugOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ArrayAdapter<String> options = new ArrayAdapter<>(this, R.layout.item_simple_text, new String[] {
                "Debug",
                "Skip"
        });
        builder.setAdapter(options, (dialog, which) -> {
            switch (which) {
                case 0: {
                    Intent intent = new Intent(this, DebugActivity.class);
                    startActivity(intent);
                    break;
                }

                case 1: {
                    showHomeActivity();
                    break;
                }

                default: {
                    break;
                }
            }
        });
        builder.setCancelable(true);
        builder.create().show();
    }

    //endregion


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
        pushFragment(new OnboardingIntroductionFragment(), null, false);
    }

    public void showSignIn() {
        pushFragment(new OnboardingSignInFragment(), null, true);
    }

    public void showRegistration() {
        pushFragment(new OnboardingRegisterFragment(), null, true);
    }

    public void showBirthday(@Nullable Account account) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        if (account != null) {
            this.account = account;
        }

        if (bluetoothAdapter.isEnabled()) {
            Logger.info(getClass().getSimpleName(), "Performing preemptive BLE Sense scan");
            bindAndSubscribe(hardwarePresenter.closestPeripheral(),
                    peripheral -> Logger.info(getClass().getSimpleName(), "Found and cached Sense " + peripheral),
                    Functions.LOG_ERROR);

            pushFragment(new OnboardingRegisterBirthdayFragment(), null, false);
        } else {
            pushFragment(OnboardingBluetoothFragment.newInstance(true), null, false);
        }
    }

    @NonNull
    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void onAccountUpdated(@NonNull AccountEditingFragment updatedBy) {
        if (updatedBy instanceof OnboardingRegisterBirthdayFragment) {
            pushFragment(new OnboardingRegisterGenderFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterGenderFragment) {
            pushFragment(new OnboardingRegisterHeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterHeightFragment) {
            pushFragment(new OnboardingRegisterWeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterWeightFragment) {
            pushFragment(new OnboardingRegisterLocationFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterLocationFragment) {
            LoadingDialogFragment.show(getFragmentManager(), getString(R.string.dialog_loading_message), true);
            bindAndSubscribe(apiService.updateAccount(account), ignored -> {
                Analytics.trackUserSignUp(account.getAccountId(), account.getName(), DateTime.now());

                LoadingDialogFragment.close(getFragmentManager());
                showEnhancedAudio();
            }, e -> {
                LoadingDialogFragment.close(getFragmentManager());
                ErrorDialogFragment.presentError(getFragmentManager(), e);
            });
        }
    }

    public void showEnhancedAudio() {
        pushFragment(new OnboardingRegisterAudioFragment(), null, false);
    }

    public void showSetupSense(boolean overrideUnsupportedDevice) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_QUESTIONS);

        if (bluetoothAdapter.isEnabled()) {
            if (!overrideUnsupportedDevice && hardwarePresenter.getDeviceSupportLevel().isUnsupported()) {
                pushFragment(new OnboardingUnsupportedDeviceFragment(), null, false);
            } else {
                OnboardingSimpleStepFragment.Builder builder = new OnboardingSimpleStepFragment.Builder(this);
                builder.setHeadingText(R.string.title_setup_sense);
                builder.setSubheadingText(R.string.info_setup_sense);
                builder.setDiagramImage(R.drawable.onboarding_sense_intro);
                builder.setNextFragmentClass(OnboardingPairSenseFragment.class);
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_SENSE_SETUP);
                builder.setHelpStep(UserSupport.OnboardingStep.SETUP_SENSE);
                pushFragment(builder.toFragment(), null, false);
            }
        } else {
            pushFragment(OnboardingBluetoothFragment.newInstance(false), null, false);
        }
    }

    public void showSelectWifiNetwork(boolean wantsBackStackEntry) {
        pushFragment(new OnboardingWifiNetworkFragment(), null, wantsBackStackEntry);
    }

    public void showSignIntoWifiNetwork(@Nullable SenseCommandProtos.wifi_endpoint network) {
        pushFragment(OnboardingSignIntoWifiFragment.newInstance(network), null, true);
    }

    public void showPairPill(boolean showIntroduction) {
        if (getIntent().getBooleanExtra(EXTRA_WIFI_CHANGE_ONLY, false)) {
            finish();
            return;
        }

        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SENSE);

        if (showIntroduction) {
            OnboardingSimpleStepFragment.Builder builder = new OnboardingSimpleStepFragment.Builder(this);
            builder.setHeadingText(R.string.onboarding_title_sleep_pill_intro);
            builder.setSubheadingText(R.string.onboarding_message_sleep_pill_intro);
            builder.setDiagramImage(R.drawable.onboarding_sleep_pill_intro);
            builder.setHideToolbar(true);
            builder.setNextFragmentClass(OnboardingPairPillFragment.class);
            pushFragment(builder.toFragment(), null, false);
        } else {
            pushFragment(new OnboardingPairPillFragment(), null, false);
        }
    }

    private OnboardingSimpleStepFragment.Builder createSenseColorsBuilder() {
        OnboardingSimpleStepFragment.Builder senseColorsBuilder = new OnboardingSimpleStepFragment.Builder(this);
        senseColorsBuilder.setHeadingText(R.string.title_sense_colors);
        senseColorsBuilder.setSubheadingText(R.string.info_sense_colors);
        senseColorsBuilder.setDiagramImage(R.drawable.onboarding_sense_colors);
        senseColorsBuilder.setHideToolbar(true);
        senseColorsBuilder.setNextWantsBackStackEntry(false);
        senseColorsBuilder.setAnalyticsEvent(Analytics.Onboarding.EVENT_SENSE_COLORS);

        OnboardingSimpleStepFragment.Builder introBuilder = new OnboardingSimpleStepFragment.Builder(this);
        introBuilder.setNextFragmentClass(OnboardingRoomCheckFragment.class);
        introBuilder.setHeadingText(R.string.onboarding_title_room_check);
        introBuilder.setSubheadingText(R.string.onboarding_info_room_check);
        introBuilder.setDiagramImage(R.drawable.onboarding_room_check);
        introBuilder.setHideToolbar(true);
        introBuilder.setExitAnimationName(ANIMATION_ROOM_CHECK);
        introBuilder.setNextWantsBackStackEntry(false);
        introBuilder.setAnalyticsEvent(Analytics.Onboarding.EVENT_ROOM_CHECK);
        senseColorsBuilder.setNextFragmentArguments(introBuilder.toArguments());
        senseColorsBuilder.setNextFragmentClass(OnboardingSimpleStepFragment.class);

        return senseColorsBuilder;
    }

    public void showPillInstructions() {
        OnboardingSimpleStepFragment.Builder builder = new OnboardingSimpleStepFragment.Builder(this);
        builder.setHeadingText(R.string.title_intro_sleep_pill);
        builder.setSubheadingText(R.string.info_intro_sleep_pill);
        builder.setDiagramImage(R.drawable.onboarding_clip_pill);
        builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_PILL_PLACEMENT);
        builder.setHelpStep(UserSupport.OnboardingStep.PILL_PLACEMENT);

        builder.setNextFragmentArguments(createSenseColorsBuilder().toArguments());
        builder.setNextFragmentClass(OnboardingSimpleStepFragment.class);

        pushFragment(builder.toFragment(), null, true);
    }

    public void showSenseColorsInfo() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_PILL);

        pushFragment(createSenseColorsBuilder().toFragment(), null, false);
    }

    public void showSmartAlarmInfo() {
        pushFragment(new OnboardingSmartAlarmFragment(), null, false);
    }

    public void show2ndPillIntroduction() {
        pushFragment(new OnboardingSetup2ndPillFragment(), null, false);
    }

    public void show2ndPillPairing() {
        pushFragment(new Onboarding2ndPillInfoFragment(), null, true);
    }

    public void showDone() {
        pushFragment(new OnboardingDoneFragment(), null, false);
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

    public @Nullable OnboardingSimpleStepFragment.ExitAnimationProvider getExitAnimationProviderNamed(@NonNull String name) {
        switch (name) {
            case ANIMATION_ROOM_CHECK: {
                return (container, onCompletion) -> {
                    animate(container)
                            .slideYAndFade(0f, getResources().getDimensionPixelSize(R.dimen.gap_outer), 1f, 0f)
                            .addOnAnimationCompleted(finished -> onCompletion.run())
                            .start();
                };
            }

            default: {
                return null;
            }
        }
    }

    //endregion
}
