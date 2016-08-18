package is.hello.sense.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.SenseOTAStatusPresenter;
import is.hello.sense.graph.presenters.UserFeaturesPresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.pill.UpdatePairPillConfirmationFragment;
import is.hello.sense.ui.fragments.pill.UpdatePairPillFragment;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.pill.UnpairPillFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.onboarding.SenseVoiceFragment;
import is.hello.sense.ui.fragments.onboarding.VoiceCompleteFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpdateReadyFragment;

public class SenseUpdateActivity extends InjectionActivity
        implements FragmentNavigation {
    public static final String ARG_NEEDS_BLUETOOTH = SenseUpdateActivity.class.getName() + ".ARG_NEEDS_BLUETOOTH";
    public static final String EXTRA_DEVICE_ID = SenseUpdateActivity.class.getName() + ".EXTRA_DEVICE_ID";
    public static final int REQUEST_CODE = 0xbeef;

    private FragmentNavigationDelegate navigationDelegate;

    private String deviceId;

    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    DeviceIssuesPresenter deviceIssuesPresenter;
    @Inject
    SenseOTAStatusPresenter senseOTAStatusPresenter;
    @Inject
    UserFeaturesPresenter userFeaturesPresenter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_onboarding_container,
                                                                 stateSafeExecutor);

        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
            getDeviceIdFromBundle(savedInstanceState);
        } else if (navigationDelegate.getTopFragment() == null) {
            showSenseUpdateIntro();
        }

        getDeviceIdFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (this.navigationDelegate == null || this.navigationDelegate.getTopFragment() == null) {
            showSenseUpdateIntro();
        }

        getDeviceIdFromIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        navigationDelegate.onSaveInstanceState(outState);
        outState.putString(EXTRA_DEVICE_ID, deviceId);
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
    public void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment, final int responseCode, @Nullable final Intent result) {
        if (responseCode == Activity.RESULT_CANCELED) {
            if (result != null && result.getBooleanExtra(ARG_NEEDS_BLUETOOTH, false)) {
                showBluetoothFragment();
            } else {
                setResult(RESULT_CANCELED, null);
                finish();
            }
            return;
        }

        if (fragment instanceof SenseUpdateIntroFragment || fragment instanceof BluetoothFragment) {
            showSenseUpdate();
        } else if (fragment instanceof PairSenseFragment) {
            if (responseCode == PairSenseFragment.REQUEST_CODE_EDIT_WIFI) {
                showSelectWifiNetwork();
            } else {
                showSenseUpdateReady();
            }
        } else if (fragment instanceof ConnectToWiFiFragment) {
            showSenseUpdateReady();
        } else if (fragment instanceof SenseUpdateReadyFragment) {
            showUnpairPillFragment();
        } else if (fragment instanceof UnpairPillFragment) {
            showUpdatePairPillFragment();
        } else if (fragment instanceof UpdatePairPillFragment) {
            showUpdatePairPillConfirmationFragment();
        } else if (fragment instanceof UpdatePairPillConfirmationFragment) {
            checkForSenseOTA();
        } else if (fragment instanceof SenseOTAIntroFragment) {
            showSenseOTAStart();
        } else if (fragment instanceof SenseOTAFragment) {
            checkHasVoiceFeature();
        } else if (fragment instanceof SenseVoiceFragment) {
            showVoiceDone();
        } else if (fragment instanceof VoiceCompleteFragment) {
            showResetOriginalSense();
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
            ((OnBackPressedInterceptor) topFragment).onInterceptBackPressed(this::back);
        } else {
            back();
        }
    }

    public void showSenseUpdateIntro() {
        pushFragment(new SenseUpdateIntroFragment(), null, false);
    }

    public void showSenseUpdate() {
        if (bluetoothStack.isEnabled()) {
            pushFragment(PairSenseFragment.newUpdateInstance(), null, false);
        } else {
            showBluetoothFragment();
        }
    }

    public void showSenseUpdateReady() {
        pushFragment(new SenseUpdateReadyFragment(), null, false);
    }

    private void showUnpairPillFragment() {
        pushFragment(new UnpairPillFragment(), null, true);
    }

    private void showUpdatePairPillFragment() {
        pushFragment(new UpdatePairPillFragment(), null, true);
    }

    private void showUpdatePairPillConfirmationFragment() {
        pushFragment(new UpdatePairPillConfirmationFragment(), null, false);
    }

    private void updatePreferences() {
        if (deviceId != null) {
            deviceIssuesPresenter.updateLastUpdatedDevice(deviceId);
        }
    }

    public void showBluetoothFragment() {
        pushFragmentAllowingStateLoss(new BluetoothFragment(), null, true);
    }

    public void showSelectWifiNetwork() {
        pushFragment(SelectWiFiNetworkFragment.newOnboardingInstance(false), null, true);
    }


    public void checkSenseOTAStatus() {
        subscribe(senseOTAStatusPresenter.storeInPrefs(),
                  Functions.NO_OP,
                  Functions.LOG_ERROR);
    }

    public void checkForSenseOTA() {
        if (senseOTAStatusPresenter.isOTARequired()) {
            showSenseUpdateIntro();
        } else {
            checkHasVoiceFeature();
        }
    }

    public void showSenseOTAIntro() {
        pushFragment(SenseOTAIntroFragment.newInstance(), null, false);
    }

    public void showSenseOTAStart() {
        pushFragment(SenseOTAFragment.newInstance(), null, false);
    }

    private void checkHasVoiceFeature() {
        if (userFeaturesPresenter.hasVoice()) {
            showSenseVoice();
        } else {
            showResetOriginalSense();
        }
    }

    private void showSenseVoice() {
        pushFragment(new SenseVoiceFragment(), null, false);
    }

    public void showVoiceDone() {
        final Fragment fragment = new VoiceCompleteFragment();
        pushFragment(fragment, null, false);
    }

    public void showResetOriginalSense() {
        pushFragment(new SenseResetOriginalFragment(), null, false);
    }

    private void back() {
        stateSafeExecutor.execute(super::onBackPressed);
    }

    private void getDeviceIdFromBundle(@NonNull final Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRA_DEVICE_ID)) {
            this.deviceId = savedInstanceState.getString(EXTRA_DEVICE_ID);
        }
    }

    private void getDeviceIdFromIntent(@Nullable final Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_DEVICE_ID)) {
            this.deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        }
    }
}
