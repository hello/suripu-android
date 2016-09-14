package is.hello.sense.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.pill.ConnectPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateIntroPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateReadyPillFragment;
import is.hello.sense.util.Analytics;

public class PillUpdateActivity extends InjectionActivity
        implements FragmentNavigation {
    public static final String ARG_NEEDS_BLUETOOTH = PillUpdateActivity.class.getName() + ".ARG_NEEDS_BLUETOOTH";
    public static final String EXTRA_DEVICE_ID = PillUpdateActivity.class.getName() + ".EXTRA_DEVICE_ID";
    public static final int REQUEST_CODE = 0xfeed;

    private FragmentNavigationDelegate navigationDelegate;
    //Todo able to remove once server can be notified immediately after a successful update.
    private String deviceId;

    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;

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
            showUpdatePillIntro();
        }

        getDeviceIdFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (this.navigationDelegate == null || this.navigationDelegate.getTopFragment() == null) {
            showUpdatePillIntro();
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

       if (fragment instanceof UpdateIntroPillFragment || fragment instanceof BluetoothFragment) {
            showConnectPillScreen();
       } else if (fragment instanceof ConnectPillFragment) {
            showUpdateReadyPill();
       } else if (fragment instanceof UpdateReadyPillFragment) {
           getDeviceIdFromIntent(result);
           updatePreferences();
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

    public void showUpdatePillIntro() {
        pushFragment(new UpdateIntroPillFragment(), null, true);
    }

    public void showConnectPillScreen() {
        pushFragment(new ConnectPillFragment(), null, false);
    }

    public void showUpdateReadyPill() {
        Analytics.trackEvent(Analytics.PillUpdate.EVENT_OTA_START, null);
        pushFragment(UpdateReadyPillFragment.newInstance(), null, false);
    }

    private void updatePreferences() {
        if (deviceId != null) {
            deviceIssuesPresenter.updateLastUpdatedDevice(deviceId);
        }
    }

    public void showBluetoothFragment() {
        pushFragmentAllowingStateLoss(new BluetoothFragment(), null, true);

    }

    private void back() {
        stateSafeExecutor.execute(super::onBackPressed);
    }

    private void getDeviceIdFromBundle(@NonNull final Bundle savedInstanceState) {
        if(savedInstanceState.containsKey(EXTRA_DEVICE_ID)){
            this.deviceId = savedInstanceState.getString(EXTRA_DEVICE_ID);
        }
    }

    private void getDeviceIdFromIntent(@Nullable final Intent intent) {
        if(intent != null && intent.hasExtra(EXTRA_DEVICE_ID)){
            this.deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        }
    }
}
