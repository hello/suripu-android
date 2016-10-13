package is.hello.sense.flows.expansions.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.flows.expansions.modules.ExpansionSettingsModule;
import is.hello.sense.flows.expansions.ui.fragments.ConfigSelectionFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionListFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;

public class ExpansionSettingsActivity extends ScopedInjectionActivity
        implements FragmentNavigation {
    private FragmentNavigationDelegate navigationDelegate;

    @Override
    protected List<Object> getModules() {
        return Collections.singletonList(new ExpansionSettingsModule());
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
            showExpansionList();
        }


    }

    //region Router

    private void showExpansionList() {
        pushFragment(new ExpansionListFragment(), null, false);
    }

    private void showExpansionDetail(final long expansionId) {
        pushFragment(ExpansionDetailFragment.newInstance(expansionId), null, true);
    }

    private void showExpansionAuth() {
        pushFragment(new ExpansionsAuthFragment(), null, true);
    }

    public void showConfigurationSelection() {
        pushFragment(new ConfigSelectionFragment(), null, true);
    }

    // end region

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
        if (responseCode == RESULT_CANCELED) {
            //todo handle
        }
        if (fragment instanceof ExpansionListFragment
                && result != null
                && result.hasExtra(ExpansionListFragment.EXPANSION_ID_KEY)) {
            showExpansionDetail(result.getLongExtra(ExpansionListFragment.EXPANSION_ID_KEY, -1));
        } else if (fragment instanceof ExpansionDetailFragment) {
            if (responseCode == ExpansionDetailFragment.RESULT_ACTION_PRESSED) {
                showExpansionAuth();
            } else if (responseCode == ExpansionDetailFragment.RESULT_CONFIGURE_PRESSED) {
                showConfigurationSelection();
            } else {
                showExpansionList();
            }
        } else if (fragment instanceof ExpansionsAuthFragment) {
            showConfigurationSelection();
        } else if (fragment instanceof ConfigSelectionFragment) {
            if (result == null || !result.hasExtra(ConfigSelectionFragment.EXPANSION_ID_KEY)) {
                showExpansionList();
            } else {
                showExpansionDetail(result.getLongExtra(ConfigSelectionFragment.EXPANSION_ID_KEY, -1));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (fragment instanceof OnBackPressedInterceptor) {
            ((OnBackPressedInterceptor) fragment).onInterceptBackPressed(stateSafeExecutor.bind(super::onBackPressed));
        } else {
            super.onBackPressed();
        }

    }
}
