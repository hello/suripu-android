package is.hello.sense.ui.activities.appcompat;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;

public abstract class FragmentNavigationActivity extends ScopedInjectionActivity
        implements FragmentNavigation {

    protected FragmentNavigationDelegate navigationDelegate;

    //region ScopedInjectionActivity
    @CallSuper
    @Override
    protected  void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);
        if (savedInstanceState != null) {
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
            onReCreateAction(savedInstanceState);
        } else {
            onCreateAction();
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (fragment instanceof OnBackPressedInterceptor) {
            ((OnBackPressedInterceptor) fragment).onInterceptBackPressed(() -> this.stateSafeExecutor.execute(super::onBackPressed));
        } else {
            super.onBackPressed();
        }
    }
    //endregion

    //region NavigationDelegate

    @Override
    public final void pushFragment(@NonNull final Fragment fragment,
                                   @Nullable final String title,
                                   final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                                    @Nullable final String title,
                                                    final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void popFragment(@NonNull final Fragment fragment,
                                  final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }


    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {

    }
    //endregion

    /**
     * Called if the activity is just starting.
     */
    protected abstract void onCreateAction();

    /**
     * Called when the activity is recreated.
     *
     * @param savedInstanceState a nonnull bundle to restore state from.
     */
    protected void onReCreateAction(@NonNull final Bundle savedInstanceState) {

    }
}
