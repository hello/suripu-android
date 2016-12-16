package is.hello.sense.flows.voice.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.flows.voice.modules.VoiceSettingsModule;
import is.hello.sense.flows.voice.ui.fragments.VoiceSettingsListFragment;
import is.hello.sense.flows.voice.ui.fragments.VoiceVolumeFragment;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;

import static is.hello.sense.flows.voice.ui.fragments.VoiceSettingsListFragment.RESULT_VOLUME_SELECTED;

public class VoiceSettingsActivity extends ScopedInjectionActivity
        implements FragmentNavigation {

    private FragmentNavigationDelegate navigationDelegate;

    @Override
    protected List<Object> getModules() {
        return Collections.singletonList(new VoiceSettingsModule());
    }


    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);

        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        } else if (navigationDelegate.getTopFragment() == null) {
            showSettingsList();
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
        this.navigationDelegate.onDestroy();
    }

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
        if(RESULT_CANCELED == responseCode){
            finish(); //todo handle better
        } else {
            if(fragment instanceof VoiceSettingsListFragment){
                if(RESULT_VOLUME_SELECTED == responseCode){
                    showVolumeSelection();
                }
            } else if(fragment instanceof VoiceVolumeFragment){
                showSettingsList();
            }
        }
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (!(fragment instanceof OnBackPressedInterceptor) ||
                !((OnBackPressedInterceptor) fragment).onInterceptBackPressed(stateSafeExecutor.bind(super::onBackPressed))) {
            stateSafeExecutor.execute(super::onBackPressed);
        }
    }

    //region Router

    private void showSettingsList(){
        pushFragment(new VoiceSettingsListFragment(), null, false);
    }

    private void showVolumeSelection(){
        pushFragment(new VoiceVolumeFragment(), null, true);
    }

    //endregion
}
