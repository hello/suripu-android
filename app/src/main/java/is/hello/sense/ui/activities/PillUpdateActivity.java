package is.hello.sense.ui.activities;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.pill.ConnectPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateIntroPillFragment;
import is.hello.sense.ui.fragments.pill.UpdateReadyPillFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class PillUpdateActivity extends InjectionActivity
implements FragmentNavigation{

    public static final int FLOW_UPDATE_PILL_INTRO_SCREEN = 3;
    public static final int FLOW_CONNECT_PILL_SCREEN = 4;
    public static final int FLOW_UPDATE_PILL_SCREEN = 5;
    public static final int FLOW_FINISHED = 6;
    public  static final int FLOW_BLUETOOTH_CHECK = 7;
    public static final int FLOW_CANCELED = 8;
    public static final int REQUEST_CODE = 0xfeed;
    public static final String EXTRA_DEVICE_ID = "device_id_extra";
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
        } else if(navigationDelegate.getTopFragment() == null){
            showUpdateIntroPill();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if(this.navigationDelegate == null || this.navigationDelegate.getTopFragment() == null){
            showUpdateIntroPill();
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
       switch (responseCode){
           case FLOW_UPDATE_PILL_INTRO_SCREEN:
               showUpdateIntroPill();
               break;
           case FLOW_BLUETOOTH_CHECK:
               showBluetoothScreen();
               break;
           case FLOW_CONNECT_PILL_SCREEN:
               showConnectPillScreen();
               break;
           case FLOW_UPDATE_PILL_SCREEN:
               showUpdateReadyPill();
               break;
           case FLOW_FINISHED:
               //Todo add fade out transition
               updatePreferences(result);
               Analytics.trackEvent(Analytics.PillUpdate.EVENT_OTA_COMPLETE, null);
               setResult(RESULT_OK);
               finish();
               break;
           case FLOW_CANCELED:
               setResult(RESULT_CANCELED, null);
               finish();
               break;
           default:
               Logger.debug(PillUpdateActivity.class.getSimpleName(),"unknown response code for flow finished.");
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

    public void showUpdateIntroPill(){
        pushFragment(UpdateIntroPillFragment.newInstance(FLOW_CONNECT_PILL_SCREEN), null, false);
    }

    private void showBluetoothScreen() {
        pushFragment(BluetoothFragment.newInstance(
                PillUpdateActivity.FLOW_CONNECT_PILL_SCREEN), null, false);
    }

    //unused but for testing or debug
    public void showConnectPillScreen(){
        pushFragment(new ConnectPillFragment(), null, false);
    }

    public void showUpdateReadyPill(){
        Analytics.trackEvent(Analytics.PillUpdate.EVENT_OTA_START, null);
        pushFragment(UpdateReadyPillFragment.newInstance(), null, false);
    }

    private void updatePreferences(@NonNull final Intent intent) {
        final String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        if(deviceId != null) {
            deviceIssuesPresenter.updateLastUpdatedDevice(deviceId);
        }
    }

    private void back(){
        stateSafeExecutor.execute(super::onBackPressed);
    }
}
