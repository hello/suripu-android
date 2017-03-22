package is.hello.sense.ui.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.notifications.OnNotificationPressedInterceptor;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.pill.ConnectPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateIntroPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateReadyPillFragment;
import is.hello.sense.util.Analytics;

public class PillUpdateActivity extends FragmentNavigationActivity
        implements OnNotificationPressedInterceptor {
    public static final String ARG_NEEDS_BLUETOOTH = PillUpdateActivity.class.getName() + ".ARG_NEEDS_BLUETOOTH";
    public static final String EXTRA_DEVICE_ID = PillUpdateActivity.class.getName() + ".EXTRA_DEVICE_ID";
    public static final int REQUEST_CODE = 0xfeed;

    //Todo able to remove once server can be notified immediately after a successful update.
    private String deviceId;

    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;

    @Override
    protected void onCreateAction() {
        showUpdatePillIntro();
        getDeviceIdFromIntent(getIntent());
    }

    @Override
    protected void onReCreateAction(@NonNull final Bundle savedInstanceState) {
        super.onReCreateAction(savedInstanceState);
        getDeviceIdFromBundle(savedInstanceState);
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
        outState.putString(EXTRA_DEVICE_ID, deviceId);
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
