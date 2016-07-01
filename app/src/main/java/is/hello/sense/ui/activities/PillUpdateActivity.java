package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.onboarding.ConnectPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepFragment;
import is.hello.sense.ui.fragments.onboarding.UpdateReadyPillFragment;
import is.hello.sense.util.Analytics;

public class PillUpdateActivity extends InjectionActivity
implements FragmentNavigation{

    private FragmentNavigationDelegate navigationDelegate;
    @Inject
    BluetoothStack bluetoothStack;

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
    protected void onSaveInstanceState(Bundle outState) {
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
        //
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

    public void showUpdateIntroPill(){
        if (bluetoothStack.isEnabled()) {
            //Todo if this activity ever needs to show exitAnimation should implement ExitAnimationProviderActivity
            final OnboardingSimpleStepFragment.Builder builder =
                    new OnboardingSimpleStepFragment.Builder(this);
            builder.setHeadingText(R.string.title_update_sleep_pill);
            builder.setSubheadingText(R.string.info_update_sleep_pill);
            builder.setDiagramImage(R.drawable.sleep_pill_ota);
            builder.setNextFragmentClass(ConnectPillFragment.class);
            builder.setAnalyticsEvent(Analytics.Onboarding.EVENT_SENSE_SETUP);
            builder.setHelpStep(UserSupport.OnboardingStep.UPDATE_PILL);
            pushFragment(builder.toFragment(), null, false);
        } else {
            pushFragment(OnboardingBluetoothFragment.newInstance(
                    OnboardingBluetoothFragment.UPDATE_PILL_SCREEN), null, false);
        }
    }

    public void showUpdateReadyPill(){
        pushFragment(UpdateReadyPillFragment.newInstance(), null, false);
    }

    private void back(){
        stateSafeExecutor.execute(super::onBackPressed);
    }
}
