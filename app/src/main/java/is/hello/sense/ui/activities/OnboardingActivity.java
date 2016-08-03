package is.hello.sense.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.util.Rx;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.DeviceOTAState;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.onboarding.HaveSenseReadyFragment;
import is.hello.sense.ui.fragments.onboarding.IntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSmartAlarmFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterCompleteFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
import is.hello.sense.ui.fragments.onboarding.SenseVoiceFragment;
import is.hello.sense.ui.fragments.onboarding.SignInFragment;
import is.hello.sense.ui.fragments.onboarding.SimpleStepFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseUpdateFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;
import static is.hello.sense.ui.activities.DebugActivity.EXTRA_DEBUG_CHECKPOINT;

public class OnboardingActivity extends InjectionActivity
        implements FragmentNavigation,
        SimpleStepFragment.ExitAnimationProviderActivity,
        AccountEditor.Container {
    public static final String TAG = OnboardingActivity.class.getName();
    public static final String EXTRA_START_CHECKPOINT = TAG + ".EXTRA_START_CHECKPOINT";
    public static final String EXTRA_PAIR_ONLY = TAG + ".EXTRA_PAIR_ONLY";
    public static final String EXTRA_RELEASE_PERIPHERAL_ON_PAIR = TAG + ".EXTRA_RELEASE_PERIPHERAL_ON_PAIR";

    public static final int FLOW_NONE = -1;
    public static final int FLOW_REGISTER = 0;
    public static final int FLOW_SIGN_IN = 1;
    private static final int EDIT_ALARM_REQUEST_CODE = 0x31;

    private static final int RESPONSE_SETUP_SENSE = 0;
    private static final int RESPONSE_SHOW_BIRTHDAY = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLOW_NONE, FLOW_REGISTER, FLOW_SIGN_IN})
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface Flow {
    }

    @Inject
    ApiService apiService;
    @Inject
    HardwarePresenter hardwarePresenter;
    @Inject
    PreferencesPresenter preferences;
    @Inject
    BluetoothStack bluetoothStack;

    private FragmentNavigationDelegate navigationDelegate;

    private
    @Nullable
    Account account;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_onboarding_container,
                                                                 stateSafeExecutor);

        if (savedInstanceState != null) {
            this.account = (Account) savedInstanceState.getSerializable("account");

            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        }

        if(BuildConfig.DEBUG){
            final int debugCheckpoint = getIntent().getIntExtra(EXTRA_DEBUG_CHECKPOINT,
                                                                Constants.DEBUG_CHECKPOINT_NONE);
            switch(debugCheckpoint){
                case Constants.DEBUG_CHECKPOINT_SENSE_UPDATE:
                    showSenseUpdateIntro();
                    break;

                case Constants.DEBUG_CHECKPOINT_SENSE_VOICE:
                    showSenseVoice();
                    break;
                case Constants.DEBUG_CHECKPOINT_NONE:
                    Log.e(TAG, "undefined debug checkpoint extra");
            }
        }

        if (getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false)) {
            final int lastCheckPoint = getLastCheckPoint();
            switch (lastCheckPoint) {
                case Constants.ONBOARDING_CHECKPOINT_SENSE:
                    showPairSense();
                    break;
                case Constants.ONBOARDING_CHECKPOINT_PILL:
                    showPairPill(false);
                    break;
            }
        } else {

            if (navigationDelegate.getTopFragment() == null) {
                final int lastCheckpoint = getLastCheckPoint();
                switch (lastCheckpoint) {
                    case Constants.ONBOARDING_CHECKPOINT_NONE:
                        showIntroductionFragment();
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_ACCOUNT:
                        if (account == null) {
                            LoadingDialogFragment.show(getFragmentManager(),
                                                       getString(R.string.dialog_loading_message),
                                                       LoadingDialogFragment.OPAQUE_BACKGROUND);
                            bindAndSubscribe(apiService.getAccount(true),
                                             nextAccount -> {
                                                 showBirthday(nextAccount, false);
                                             },
                                             e -> {
                                                 LoadingDialogFragment.close(getFragmentManager());
                                                 ErrorDialogFragment.presentError(this, e);
                                             });
                        } else {
                            showBirthday(account, false);
                        }
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_QUESTIONS:
                        showSetupSense();
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_SENSE:
                        showPairPill(!getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false));
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_PILL:
                        showSenseColorsInfo();
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_SMART_ALARM:
                        showSmartAlarmInfo();
                        break;
                }
            }
        }

        //Really not sure why this would ever be needed unless for debugging
        // This is the only place that would be problematic with starting Launch Activity from Sense Application
        final Observable<Intent> onLogOut =
                Rx.fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        subscribe(onLogOut,
                  ignored -> {
                      // #recreate() doesn't work. This does.
                      final Intent intent = getIntent();
                      finish();
                      startActivity(intent);
                  },
                  Functions.LOG_ERROR);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("account", account);
        navigationDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_ALARM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                checkForSenseUpdate();
            } else {
                showSmartAlarmInfo();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        navigationDelegate.onDestroy();
    }

    @Override
    public void pushFragment(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void popFragment(@NonNull final Fragment fragment,
                            final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {
        if (fragment instanceof IntroductionFragment) {
            if (responseCode == IntroductionFragment.RESPONSE_SIGN_IN) {
                showSignIn();
            } else if (responseCode == IntroductionFragment.RESPONSE_GET_STARTED) {
                showGetStarted(false);
            }
        } else if (fragment instanceof ConnectToWiFiFragment) {
            showPairPill(true);
        } else if (fragment instanceof BluetoothFragment) {
            if(responseCode == OnboardingActivity.RESPONSE_SETUP_SENSE){
                showSetupSense();
            } else if(responseCode == OnboardingActivity.RESPONSE_SHOW_BIRTHDAY){
                showBirthday(null, true);
            }
        } else if(fragment instanceof OnboardingRoomCheckFragment ||
                fragment instanceof OnboardingSenseColorsFragment) {
            checkSenseUpdateStatus();
            showSmartAlarmInfo();
        } else if (fragment instanceof SenseUpdateFragment) {
            if (responseCode == Activity.RESULT_CANCELED) {
                showDone();
            } else if (responseCode == Activity.RESULT_OK) {
                showSenseVoice(); //todo api check for voice feature
            }
        } else if (fragment instanceof SenseVoiceFragment) {
            showDone();
        }
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public void onBackPressed() {
        final Fragment topFragment = getTopFragment();
        if (topFragment instanceof OnBackPressedInterceptor) {
            if (((OnBackPressedInterceptor) topFragment).onInterceptBackPressed(this::back)) {
                return;
            }
        }

        back();
    }

    public void back() {
        final boolean hasStartCheckPoint = getIntent().hasExtra(EXTRA_START_CHECKPOINT);
        final boolean pairOnly = getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false);
        final boolean wantsDialog = (!hasStartCheckPoint && !pairOnly);
        if (wantsDialog && getFragmentManager().getBackStackEntryCount() == 0) {
            final SenseAlertDialog builder = new SenseAlertDialog(this);
            builder.setTitle(R.string.dialog_title_confirm_leave_onboarding);
            builder.setMessage(R.string.dialog_message_confirm_leave_onboarding);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> stateSafeExecutor.execute(super::onBackPressed));
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        } else {
            stateSafeExecutor.execute(super::onBackPressed);
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

    public void passedCheckPoint(final int checkPoint) {
        preferences
                .edit()
                .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, checkPoint)
                .apply();

        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void showIntroductionFragment() {
        pushFragment(new IntroductionFragment(), null, false);
    }

    public void showSignIn() {
        pushFragment(new SignInFragment(), null, true);
    }

    public void showPairSense() {
        if (bluetoothStack.isEnabled()) {
            pushFragment(new OnboardingPairSenseFragment(), null, false);
        } else {
            pushFragment(BluetoothFragment.newInstance(
                    OnboardingActivity.RESPONSE_SETUP_SENSE), null, false);
        }
    }

    public void showGetStarted(final boolean overrideDeviceUnsupported) {
        if (!overrideDeviceUnsupported && !hardwarePresenter.isDeviceSupported()) {
            pushFragment(new OnboardingUnsupportedDeviceFragment(), null, true);
        } else {
            pushFragment(new HaveSenseReadyFragment(), null, true);
        }
    }

    public void showBirthday(@Nullable final Account account, final boolean withDoneTransition) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        if (account != null) {
            this.account = account;
        }

        if (bluetoothStack.isEnabled()) {
            Logger.info(TAG, "Performing preemptive BLE Sense scan");
            bindAndSubscribe(hardwarePresenter.closestPeripheral(),
                             peripheral -> Logger.info(TAG,
                                                       "Found and cached Sense " + peripheral),
                             Functions.IGNORE_ERROR);

            pushFragment(new OnboardingRegisterBirthdayFragment(), null, false);
        } else {
            pushFragment(BluetoothFragment.newInstance(
                    OnboardingActivity.RESPONSE_SHOW_BIRTHDAY), null, false);
        }

        if (withDoneTransition) {
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
        } else {
            LoadingDialogFragment.close(getFragmentManager());
        }
    }

    @NonNull
    @Override
    public Account getAccount() {
        if (account == null) {
            Logger.warn(TAG, "getAccount() without account being specified before-hand. Creating default.");
            this.account = Account.createDefault();
        }

        return account;
    }

    @Override
    public void onAccountUpdated(@NonNull final SenseFragment updatedBy) {
        if (updatedBy instanceof OnboardingRegisterBirthdayFragment) {
            pushFragment(new OnboardingRegisterGenderFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterGenderFragment) {
            pushFragment(new OnboardingRegisterHeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterHeightFragment) {
            pushFragment(new OnboardingRegisterWeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterWeightFragment) {
            final Account account = getAccount();
            bindAndSubscribe(apiService.updateAccount(account, true), ignored -> {
                LoadingDialogFragment.close(getFragmentManager());
                showEnhancedAudio();
            }, e -> {
                LoadingDialogFragment.close(getFragmentManager());
                ErrorDialogFragment.presentError(this, e);
            });
        }
    }

    public void showEnhancedAudio() {
        pushFragment(new OnboardingRegisterAudioFragment(), null, false);
    }

    public void showSetupSense() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_QUESTIONS);

        if (bluetoothStack.isEnabled()) {
            final SimpleStepFragment.Builder builder =
                    new SimpleStepFragment.Builder(this);
            builder.setHeadingText(R.string.title_setup_sense);
            builder.setSubheadingText(R.string.info_setup_sense);
            builder.setDiagramImage(R.drawable.onboarding_sense_intro);
            builder.setNextFragmentClass(OnboardingPairSenseFragment.class);
            if (getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false)) {
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_SENSE_SETUP_IN_APP);
            } else {
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_SENSE_SETUP);
            }
            builder.setHelpStep(UserSupport.OnboardingStep.SETTING_UP_SENSE);
            pushFragment(builder.toFragment(), null, false);
        } else {
            pushFragment(BluetoothFragment.newInstance(
                    OnboardingActivity.RESPONSE_SETUP_SENSE), null, false);
        }
    }

    public void showSelectWifiNetwork() {
        final boolean pairOnly = getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false);
        pushFragment(SelectWiFiNetworkFragment.newOnboardingInstance(pairOnly), null, true);
    }

    public void showPairPill(final boolean showIntroduction) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SENSE);

        if (showIntroduction) {
            bindAndSubscribe(apiService.devicesInfo(),
                             devicesInfo -> {
                                 Logger.info(TAG, "Loaded devices info");
                                 Analytics.setSenseId(devicesInfo.getSenseId());
                             }, e -> {
                        Logger.error(TAG, "Failed to silently load devices info, will retry later", e);
                    });

            final SimpleStepFragment.Builder builder =
                    new SimpleStepFragment.Builder(this);
            builder.setHeadingText(R.string.onboarding_title_sleep_pill_intro);
            builder.setSubheadingText(R.string.onboarding_message_sleep_pill_intro);
            builder.setDiagramImage(R.drawable.onboarding_sleep_pill);
            builder.setHideToolbar(true);
            if (getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false)) {
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_PILL_INTRO_IN_APP);
            } else {
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_PILL_INTRO);
            }
            builder.setNextFragmentClass(OnboardingPairPillFragment.class);
            pushFragment(builder.toFragment(), null, false);
        } else {
            pushFragment(new OnboardingPairPillFragment(), null, false);
        }
    }

    public void showPillInstructions() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_PILL);

        final SimpleStepFragment.Builder builder =
                new SimpleStepFragment.Builder(this);
        builder.setHeadingText(R.string.title_intro_sleep_pill);
        builder.setSubheadingText(R.string.info_intro_sleep_pill);
        builder.setDiagramVideo(Uri.parse(getString(R.string.diagram_onboarding_clip_pill)));
        builder.setDiagramImage(R.drawable.onboarding_clip_pill);
        builder.setCompact(true);
        builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_PILL_PLACEMENT);
        builder.setHelpStep(UserSupport.OnboardingStep.PILL_PLACEMENT);

        builder.setNextFragmentClass(OnboardingSenseColorsFragment.class);

        pushFragment(builder.toFragment(), null, true);
    }

    public void showSenseColorsInfo() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_PILL);

        pushFragment(new OnboardingSenseColorsFragment(), null, false);
    }

    public void showRoomCheckIntro() {
        final SimpleStepFragment.Builder introBuilder =
                new SimpleStepFragment.Builder(this);
        introBuilder.setNextFragmentClass(OnboardingRoomCheckFragment.class);
        introBuilder.setHeadingText(R.string.onboarding_title_room_check);
        introBuilder.setSubheadingText(R.string.onboarding_info_room_check);
        introBuilder.setDiagramImage(R.drawable.onboarding_sense_intro);
        introBuilder.setHideToolbar(true);
        introBuilder.setExitAnimationName(ANIMATION_ROOM_CHECK);
        introBuilder.setNextWantsBackStackEntry(false);
        introBuilder.setAnalyticsEvent(Analytics.Onboarding.EVENT_ROOM_CHECK);
        pushFragment(introBuilder.toFragment(), null, true);
    }

    public void showSmartAlarmInfo() {
        pushFragment(new OnboardingSmartAlarmFragment(), null, false);
    }

    public void showSetAlarmDetail(){
        pushFragment(new Fragment(), null, false);
        final Intent newAlarm = new Intent(this, SmartAlarmDetailActivity.class);
        startActivityForResult(newAlarm, EDIT_ALARM_REQUEST_CODE);
    }

    public void checkSenseUpdateStatus(){
        subscribe(apiService.getSenseUpdateStatus(),
                  otaStatus -> {
                      Log.d(TAG, "checkSenseUpdateStatus: " + otaStatus.state.name());
                      preferences.edit().putString(PreferencesPresenter.DEVICE_OTA_STATUS, otaStatus.state.name())
                                 .apply();
                  },
                  Functions.LOG_ERROR);
    }

    public void checkForSenseUpdate() {
        final String senseOtaStatus = preferences.getString(PreferencesPresenter.DEVICE_OTA_STATUS,"missing");
        if(senseOtaStatus.equals(DeviceOTAState.OtaState.REQUIRED.name())){
            showSenseUpdateIntro();
        } else{
            showDone();
        }
    }

    public void showSenseUpdateIntro(){
        pushFragment(SenseUpdateIntroFragment.newInstance(), null, false);
    }

    public void showSenseUpdating(){
        pushFragment(SenseUpdateFragment.newInstance(), null, false);
    }

    private void showSenseVoice() {
        pushFragment(new SenseVoiceFragment(), null, false);
    }

    public void showDone() {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SMART_ALARM);
        final Fragment fragment = new RegisterCompleteFragment();
        pushFragment(fragment, null, false);
    }

    //endregion


    public void showHomeActivity(@Flow final int fromFlow) {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, true)
                   .apply();

        hardwarePresenter.clearPeripheral();

        final Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_ONBOARDING_FLOW, fromFlow);
        startActivity(intent);
        finish();
    }

    //region Static Step Animation

    public static final String ANIMATION_ROOM_CHECK = "room_check";

    @Override
    public
    @Nullable
    SimpleStepFragment.ExitAnimationProvider getExitAnimationProviderNamed(@NonNull final String name) {
        switch (name) {
            case ANIMATION_ROOM_CHECK: {
                return (view, onCompletion) -> {
                    final int slideAmount = getResources().getDimensionPixelSize(R.dimen.gap_xlarge);

                    animatorFor(view.contentsScrollView)
                            .slideYAndFade(0f, -slideAmount, 1f, 0f)
                            .start();

                    animatorFor(view.primaryButton)
                            .slideYAndFade(0f, slideAmount, 1f, 0f)
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
