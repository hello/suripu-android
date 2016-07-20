package is.hello.sense.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.pill.PillHardwareFragment;
import is.hello.sense.ui.fragments.pill.PillUpdateFragment;
import is.hello.sense.ui.fragments.pill.ConnectPillFragment;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.ui.fragments.pill.UpdateReadyPillFragment;
import is.hello.sense.util.Logger;

public class PillUpdateActivity extends InjectionActivity
        implements FragmentNavigation {
    public static final String ARG_NEEDS_BLUETOOTH = PillUpdateActivity.class.getName() + ".ARG_NEEDS_BLUETOOTH";
    public static final String EXTRA_DEVICE_ID = PillUpdateActivity.class.getName() + ".EXTRA_DEVICE_ID";
    public static final int REQUEST_CODE = 0xfeed;

    private FragmentNavigationDelegate navigationDelegate;

    @Inject
    DeviceIssuesPresenter deviceIssuesPresenter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_onboarding_container,
                                                                 stateSafeExecutor);

        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        } else if (navigationDelegate.getTopFragment() == null) {
            showUpdatePillIntro();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (this.navigationDelegate == null || this.navigationDelegate.getTopFragment() == null) {
            showUpdatePillIntro();
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
    }

    @Override
    public void onBackPressed() {
        final Fragment topFragment = getTopFragment();
        if (topFragment instanceof OnBackPressedInterceptor) {
            if (((OnBackPressedInterceptor) topFragment).onInterceptBackPressed(this::back)) {
                return;
            }
        } else if (topFragment instanceof BluetoothFragment) {
            showUpdatePillIntro();
            return;
        } else if (topFragment instanceof ConnectPillFragment) {
            return;
        }

        back();
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
                finish();
            }
            return;
        }

       if (fragment instanceof PillUpdateFragment || fragment instanceof BluetoothFragment) {
            showConnectPillScreen();
        } else if (fragment instanceof ConnectPillFragment) {
            showUpdateReadyPill();
        } else if (fragment instanceof UpdateReadyPillFragment) {
            if (result != null) {
                updatePreferences(result);
            }
            Analytics.trackEvent(Analytics.PillUpdate.EVENT_OTA_COMPLETE, null);
            setResult(RESULT_OK);
            finish();
        }
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    public void showUpdatePillIntro() {
        pushFragmentAllowingStateLoss(new PillUpdateFragment(), null, false);
    }

    public void showConnectPillScreen() {
        pushFragment(new ConnectPillFragment(), null, false);
    }

    public void showUpdateReadyPill() {
        Analytics.trackEvent(Analytics.PillUpdate.EVENT_OTA_START, null);
        pushFragment(UpdateReadyPillFragment.newInstance(), null, false);
    }

    private void updatePreferences(@NonNull final Intent intent) {
        final String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        if (deviceId != null) {
            deviceIssuesPresenter.updateLastUpdatedDevice(deviceId);
        }
    }

    public void showBluetoothFragment() {
        pushFragmentAllowingStateLoss(new BluetoothFragment(), null, false);

    }

    private void back() {
        stateSafeExecutor.execute(super::onBackPressed);
    }
}
