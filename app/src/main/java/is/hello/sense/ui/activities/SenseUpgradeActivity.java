package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.SenseUpgradeModule;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.SenseOTAStatusInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.SenseVoiceFragment;
import is.hello.sense.ui.fragments.onboarding.VoiceCompleteFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAFragment;
import is.hello.sense.ui.fragments.onboarding.sense.SenseOTAIntroFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;
import is.hello.sense.ui.fragments.pill.UnpairPillFragment;
import is.hello.sense.ui.fragments.pill.UpdatePairPillConfirmationFragment;
import is.hello.sense.ui.fragments.sense.SenseResetOriginalFragment;
import is.hello.sense.ui.fragments.sense.SenseUpgradeIntroFragment;
import is.hello.sense.ui.fragments.sense.SenseUpgradeReadyFragment;
import is.hello.sense.ui.fragments.updating.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;
import is.hello.sense.util.SkippableFlow;

public class SenseUpgradeActivity extends ScopedInjectionActivity
        implements FragmentNavigation, SkippableFlow {
    public static final String ARG_NEEDS_BLUETOOTH = SenseUpgradeActivity.class.getName() + ".ARG_NEEDS_BLUETOOTH";

    private FragmentNavigationDelegate navigationDelegate;

    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    SenseOTAStatusInteractor senseOTAStatusPresenter;
    @Inject
    UserFeaturesInteractor userFeaturesPresenter;
    @Inject
    CurrentSenseInteractor currentSenseInteractor;
    @Inject
    DevicesInteractor devicesInteractor;

    @Override
    protected List<Object> getModules() {
        return Collections.singletonList(new SenseUpgradeModule());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_onboarding_container,
                                                                 stateSafeExecutor);
        devicesInteractor.update();
        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        } else if (navigationDelegate.getTopFragment() == null) {
            showSenseUpdateIntro();
            storeCurrentSenseDevice();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (this.navigationDelegate == null || this.navigationDelegate.getTopFragment() == null) {
            showSenseUpdateIntro();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        navigationDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationDelegate.onDestroy();
        destroyScopedGraph();
    }

    //region SkippableFlow interface

    @Override
    public void skipToEnd() {
        showHomeActivity();
    }

    //endregion

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
        if (responseCode == RESULT_CANCELED) {
            if (result != null && result.getBooleanExtra(ARG_NEEDS_BLUETOOTH, false)) {
                showBluetoothFragment();
            } else if ( fragment instanceof PairSenseFragment){
                showSenseUpdateIntro();
            } else if (fragment instanceof UnpairPillFragment || fragment instanceof PairPillFragment){
                checkForSenseOTA();
            } else if ( fragment instanceof SenseOTAFragment) {
                checkHasVoiceFeature();
            } else if (fragment instanceof SenseVoiceFragment){
                showVoiceDone();
            } else {
                setResult(RESULT_CANCELED, null);
                finish();
            }
            return;
        }

        if (fragment instanceof SenseUpgradeIntroFragment || fragment instanceof BluetoothFragment) {
            showSenseUpdate();
        } else if (fragment instanceof PairSenseFragment) {
            if (responseCode == PairSensePresenter.REQUEST_CODE_EDIT_WIFI) {
                showSelectWifiNetwork();
            } else {
                showSenseUpdateReady();
            }
        } else if (fragment instanceof ConnectToWiFiFragment) {
            showSenseUpdateReady();
        } else if (fragment instanceof SenseUpgradeReadyFragment) {
            checkSenseOTAStatus();
            showUnpairPillFragment();
        } else if (fragment instanceof UnpairPillFragment) {
            showUpdatePairPillFragment();
        } else if (fragment instanceof PairPillFragment) {
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
        } else if (fragment instanceof SenseResetOriginalFragment) {
            showHomeActivity();
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

    public void storeCurrentSenseDevice() {
        currentSenseInteractor.update();
    }

    public void showSenseUpdateIntro() {
        pushFragment(new SenseUpgradeIntroFragment(), null, false);
    }

    public void showSenseUpdate() {
        if (bluetoothStack.isEnabled()) {
            pushFragment(new PairSenseFragment(), null, false);
        } else {
            showBluetoothFragment();
        }
    }

    public void showSenseUpdateReady() {
        pushFragment(new SenseUpgradeReadyFragment(), null, false);
    }

    private void showUnpairPillFragment() {
        pushFragment(new UnpairPillFragment(), null, true);
    }

    private void showUpdatePairPillFragment() {
        pushFragment(new PairPillFragment(), null, false);
    }

    private void showUpdatePairPillConfirmationFragment() {
        pushFragment(new UpdatePairPillConfirmationFragment(), null, false);
    }

    private void updatePreferences() {
        //todo uncomment when able to store new device id
        //deviceIssuesPresenter.updateLastUpdatedDevice(newDeviceId);
    }

    public void showBluetoothFragment() {
        pushFragmentAllowingStateLoss(new BluetoothFragment(), null, true);
    }

    public void showSelectWifiNetwork() {
        pushFragment(new SelectWifiNetworkFragment(), null, true);
    }


    public void checkSenseOTAStatus() {
        subscribe(senseOTAStatusPresenter.storeInPrefs(),
                  Functions.NO_OP,
                  Functions.LOG_ERROR);
    }

    public void checkForSenseOTA() {
        if (senseOTAStatusPresenter.isOTARequired()) {
            showSenseOTAIntro();
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
        // todo a bluetooth check needs to be added before deciding to push
        pushFragment(new SenseResetOriginalFragment(), null, false);
    }

    public void showHomeActivity() {
        setResult(RESULT_OK);
        final Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void back() {
        stateSafeExecutor.execute(super::onBackPressed);
    }

}
