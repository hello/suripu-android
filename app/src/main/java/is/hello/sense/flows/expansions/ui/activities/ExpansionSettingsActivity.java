package is.hello.sense.flows.expansions.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.modules.ExpansionSettingsModule;
import is.hello.sense.flows.expansions.routers.ExpansionSettingsRouter;
import is.hello.sense.flows.expansions.ui.fragments.ConfigSelectionFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionListFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;

public class ExpansionSettingsActivity extends ScopedInjectionActivity
        implements FragmentNavigation, ExpansionSettingsRouter{
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

        if(savedInstanceState != null){
            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        }
        else if(navigationDelegate.getTopFragment() == null){
            showExpansionList();
        }

    }

    //region Router

    @Override
    public void showExpansionList() {
        pushFragment(new ExpansionListFragment(), null, false);
    }

    @Override
    public void showExpansionDetail(final long expansionId){
        pushFragment(ExpansionDetailFragment.newInstance(expansionId), null, true);
    }

    @Override
    public void showExpansionAuth(final long expansionId,
                                  @NonNull final String initialUrl,
                                  @NonNull final String completionUrl) {
        pushFragment(ExpansionsAuthFragment.newInstance(expansionId, initialUrl, completionUrl), null, true);
    }

    @Override
    public void showConfigurationSelection(@NonNull final Expansion expansion){
        pushFragment(ConfigSelectionFragment.newInstance(expansion), null, true);
    }

    @Override
    public void showConfigurationSelection(final long expansionId) {
        pushFragment(ConfigSelectionFragment.newInstance(expansionId), null, true);
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
        if(responseCode != RESULT_OK){
            //todo handle
        }
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if(fragment instanceof OnBackPressedInterceptor){
            ((OnBackPressedInterceptor) fragment).onInterceptBackPressed(stateSafeExecutor.bind(super::onBackPressed));
        } else{
            super.onBackPressed();
        }

    }
}
