package is.hello.sense.flows.settings.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.settings.ui.fragments.AppSettingsFragment;
import is.hello.sense.flows.settings.ui.fragments.NotificationFragment;
import is.hello.sense.flows.settings.ui.views.AppSettingsView;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.activities.appcompat.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Share;

public class AppSettingsActivity extends ScopedInjectionActivity
        implements FragmentNavigation {
    private FragmentNavigationDelegate navigationDelegate;

    //region ScopedInjectionActivity
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);
        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        } else {
            showAppSettingsFragment();
        }
        updateActionBar();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        navigationDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.navigationDelegate.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        updateActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
    //endregion

    //region NavigationDelegate

    @Override
    public final void pushFragment(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public final void flowFinished(@NonNull final Fragment fragment, final int responseCode, @Nullable final Intent result) {
        if (fragment instanceof AppSettingsFragment) {
            switch (responseCode) {
                case AppSettingsView.INDEX_ACCOUNT:
                    showAccountSettingsFragment();
                    break;
                case AppSettingsView.INDEX_DEVICES:
                    showDevicesFragment();
                    break;
                case AppSettingsView.INDEX_NOTIFICATIONS:
                    showNotificationsFragment();
                    break;
                case AppSettingsView.INDEX_EXPANSIONS:
                    showExpansions();
                    break;
                case AppSettingsView.INDEX_VOICE:
                    showVoiceFragment();
                    break;
                case AppSettingsView.INDEX_SUPPORT:
                    showSupportFragment();
                    break;
                case AppSettingsView.INDEX_SHARE:
                    showShare();
                    break;
                case AppSettingsView.INDEX_DEBUG:
                    showDebugActivity();
                    break;

            }
        }
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }
    //endregion

    //region methods
    public void showAppSettingsFragment() {
        pushFragment(new AppSettingsFragment(), null, false);
    }

    public void showAccountSettingsFragment() {
        showFragment(AccountSettingsFragment.class, R.string.label_account, true);
    }

    public void showDevicesFragment() {
        showFragment(DeviceListFragment.class, R.string.label_devices, false);

    }

    public void showNotificationsFragment() {
        pushFragment(new NotificationFragment(), null, true);
        updateActionBar();
    }

    public void showExpansions() {
        startActivity(new Intent(this, ExpansionSettingsActivity.class));
    }

    public void showVoiceFragment() {
        startActivity(new Intent(this, VoiceSettingsActivity.class));
    }

    public void showSupportFragment() {
        showFragment(SupportFragment.class, R.string.action_support, false);
    }

    public void showShare() {
        Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .send(this);
    }

    public void showDebugActivity() {
        Distribution.startDebugActivity(this);
    }

    private void updateActionBar() {
        final Fragment topFragment = getTopFragment();
        if (topFragment instanceof NotificationFragment) {
            setActionBarText(R.string.label_notifications);
            setActionBarHomeUpImage(R.drawable.app_style_ab_cancel);
        } else {
            setActionBarText(R.string.app_name);
            setActionBarHomeUpImage(R.drawable.app_style_ab_up);
        }
    }

    private void showFragment(@NonNull final Class<? extends Fragment> fragmentClass,
                              @StringRes final int titleRes,
                              final boolean lockOrientation) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(this, HardwareFragmentActivity.class);
        builder.setDefaultTitle(titleRes);
        builder.setFragmentClass(fragmentClass);
        if (lockOrientation) {
            builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        startActivity(builder.toIntent());

    }
    //endregion

}
