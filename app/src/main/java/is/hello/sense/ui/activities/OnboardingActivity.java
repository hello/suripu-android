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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
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
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.onboarding.HaveSenseReadyFragment;
import is.hello.sense.ui.fragments.onboarding.IntroductionFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSmartAlarmFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterCompleteFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
import is.hello.sense.ui.fragments.onboarding.SignInFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class OnboardingActivity extends InjectionActivity
        implements FragmentNavigation, AccountEditor.Container {
    public static final String EXTRA_START_CHECKPOINT = OnboardingActivity.class.getName() + ".EXTRA_START_CHECKPOINT";
    public static final String EXTRA_PAIR_ONLY = OnboardingActivity.class.getName() + ".EXTRA_PAIR_ONLY";
    public static final String EXTRA_SHOW_SENSE_PAIR_ONLY = OnboardingActivity.class.getName() + ".EXTRA_SHOW_SENSE_PAIR_ONLY";

    public static final int FLOW_NONE = -1;
    public static final int FLOW_REGISTER = 0;
    public static final int FLOW_SIGN_IN = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLOW_NONE, FLOW_REGISTER, FLOW_SIGN_IN})
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface Flow {}

    @Inject ApiService apiService;
    @Inject HardwarePresenter hardwarePresenter;
    @Inject PreferencesPresenter preferences;
    @Inject BluetoothStack bluetoothStack;

    private FragmentNavigationDelegate navigationDelegate;

    private @Nullable Account account;

    public static void startActivityForPairingSense(@NonNull final Activity from){
        Intent intent = new Intent(from, OnboardingActivity.class);
        intent.putExtra(EXTRA_SHOW_SENSE_PAIR_ONLY, true);
        intent.putExtra(EXTRA_PAIR_ONLY, true);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_onboarding_container,
                                                                 stateSafeExecutor);
        boolean showSensePairOnly = false;
        if (getIntent() != null){
            showSensePairOnly = getIntent().getBooleanExtra(EXTRA_SHOW_SENSE_PAIR_ONLY, false);
        }
        if (savedInstanceState != null) {
            this.account = (Account) savedInstanceState.getSerializable("account");

            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        }

        if (showSensePairOnly) {
            showPairSense();
        } else {
            if (navigationDelegate.getTopFragment() == null) {
                int lastCheckpoint = getLastCheckPoint();
                switch (lastCheckpoint) {
                    case Constants.ONBOARDING_CHECKPOINT_NONE:
                        showIntroductionFragment();
                        break;

                    case Constants.ONBOARDING_CHECKPOINT_ACCOUNT:
                        if (account == null) {
                            LoadingDialogFragment.show(getFragmentManager(),
                                                       getString(R.string.dialog_loading_message),
                                                       LoadingDialogFragment.OPAQUE_BACKGROUND);
                            bindAndSubscribe(apiService.getAccount(),
                                             account -> {
                                                 showBirthday(account, false);
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
                }
            }
        }

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("account", account);
        navigationDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        navigationDelegate.onDestroy();
    }

    @Override
    public void pushFragment(@NonNull Fragment fragment, @Nullable String title, boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull Fragment fragment, @Nullable String title, boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void popFragment(@NonNull Fragment fragment,
                            boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull Fragment fragment,
                             int responseCode,
                             @Nullable Intent result) {
        if (fragment instanceof IntroductionFragment) {
            if (responseCode == IntroductionFragment.RESPONSE_SIGN_IN) {
                showSignIn();
            } else if (responseCode == IntroductionFragment.RESPONSE_GET_STARTED) {
                showGetStarted(false);
            }
        } else if (fragment instanceof ConnectToWiFiFragment) {
            showPairPill(true);
        }
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public void onBackPressed() {
        Fragment topFragment = getTopFragment();
        if (topFragment instanceof OnBackPressedInterceptor) {
            if (((OnBackPressedInterceptor) topFragment).onInterceptBackPressed(this::back)) {
                return;
            }
        }

        back();
    }

    public void back() {
        boolean hasStartCheckPoint = getIntent().hasExtra(EXTRA_START_CHECKPOINT);
        boolean pairOnly = getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false);
        boolean wantsDialog = (!hasStartCheckPoint && !pairOnly);
        if (wantsDialog && getFragmentManager().getBackStackEntryCount() == 0) {
            SenseAlertDialog builder = new SenseAlertDialog(this);
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

    public void passedCheckPoint(int checkPoint) {
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

    public void showGetStarted(boolean overrideDeviceUnsupported) {
        if (!overrideDeviceUnsupported && !hardwarePresenter.isDeviceSupported()) {
            pushFragment(new OnboardingUnsupportedDeviceFragment(), null, true);
        } else {
            pushFragment(new HaveSenseReadyFragment(), null, true);
        }
    }

    public void showBirthday(@Nullable Account account, boolean withDoneTransition) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_ACCOUNT);

        if (account != null) {
            this.account = account;
        }

        if (bluetoothStack.isEnabled()) {
            Logger.info(getClass().getSimpleName(), "Performing preemptive BLE Sense scan");
            bindAndSubscribe(hardwarePresenter.closestPeripheral(),
                             peripheral -> Logger.info(getClass().getSimpleName(),
                                                       "Found and cached Sense " + peripheral),
                             Functions.IGNORE_ERROR);

            pushFragment(new OnboardingRegisterBirthdayFragment(), null, false);
        } else {
            pushFragment(OnboardingBluetoothFragment.newInstance(true), null, false);
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
            Logger.warn(getClass().getSimpleName(), "getAccount() without account being specified before-hand. Creating default.");
            this.account = Account.createDefault();
        }

        return account;
    }

    @Override
    public void onAccountUpdated(@NonNull SenseFragment updatedBy) {
        if (updatedBy instanceof OnboardingRegisterBirthdayFragment) {
            pushFragment(new OnboardingRegisterGenderFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterGenderFragment) {
            pushFragment(new OnboardingRegisterHeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterHeightFragment) {
            pushFragment(new OnboardingRegisterWeightFragment(), null, true);
        } else if (updatedBy instanceof OnboardingRegisterWeightFragment) {
            Account account = getAccount();
            bindAndSubscribe(apiService.updateAccount(account), ignored -> {
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
            final OnboardingSimpleStepFragment.Builder builder =
                    new OnboardingSimpleStepFragment.Builder(this);
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
            pushFragment(OnboardingBluetoothFragment.newInstance(false), null, false);
        }
    }

    public void showPairSense() {
        if (bluetoothStack.isEnabled()) {
            pushFragment(new OnboardingPairSenseFragment(), null, false);
        } else {
            pushFragment(OnboardingBluetoothFragment.newInstance(false), null, false);
        }
    }

    public void showSelectWifiNetwork() {
        boolean pairOnly = getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false);
        pushFragment(SelectWiFiNetworkFragment.newOnboardingInstance(pairOnly), null, true);
    }

    public void showPairPill(boolean showIntroduction) {
        passedCheckPoint(Constants.ONBOARDING_CHECKPOINT_SENSE);

        if (showIntroduction) {
            bindAndSubscribe(apiService.devicesInfo(),
                             devicesInfo -> {
                                 Logger.info(getClass().getSimpleName(), "Loaded devices info");
                                 Analytics.setSenseId(devicesInfo.getSenseId());
                             }, e -> {
                                 Logger.error(getClass().getSimpleName(), "Failed to silently load devices info, will retry later", e);
                             });

            final OnboardingSimpleStepFragment.Builder builder =
                    new OnboardingSimpleStepFragment.Builder(this);
            builder.setHeadingText(R.string.onboarding_title_sleep_pill_intro);
            builder.setSubheadingText(R.string.onboarding_message_sleep_pill_intro);
            builder.setDiagramImage(R.drawable.onboarding_sleep_pill);
            builder.setHideToolbar(true);
            if (getIntent().getBooleanExtra(EXTRA_PAIR_ONLY, false)) {
                builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_PILL_INTRO_IN_APP);
            }else{
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

        final OnboardingSimpleStepFragment.Builder builder =
                new OnboardingSimpleStepFragment.Builder(this);
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
        final OnboardingSimpleStepFragment.Builder introBuilder =
                new OnboardingSimpleStepFragment.Builder(this);
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

    public void showDone() {
        pushFragment(new RegisterCompleteFragment(), null, false);
    }

    //endregion


    public void showHomeActivity(@Flow int fromFlow) {
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

    public @Nullable OnboardingSimpleStepFragment.ExitAnimationProvider getExitAnimationProviderNamed(@NonNull String name) {
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
