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
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.modules.ExpansionSettingsModule;
import is.hello.sense.flows.expansions.ui.fragments.ConfigSelectionFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionListFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;

public class ExpansionSettingsActivity extends ScopedInjectionActivity
implements FragmentNavigation{
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

    private void showExpansionList() {
        pushFragment(new ExpansionListFragment(), null, false);
    }

    private void showExpansionDetail(@NonNull final Expansion expansion){
        pushFragment(ExpansionDetailFragment.newInstance(expansion), null, true);
    }

    private void showExpansionAuth(@NonNull final String initialUrl,
                                   @NonNull final String completionUrl) {
        pushFragment(ExpansionsAuthFragment.newInstance(initialUrl, completionUrl), null, true);
    }

    private void showConfigurationSelection(){
        //todo remove after testing
        pushFragment(ConfigSelectionFragment.newInstance(Expansion.generateThermostatTestCase(State.NOT_CONFIGURED)), null, true);
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
        if(responseCode != RESULT_OK){
            //todo handle
        } else if(fragment instanceof ExpansionListFragment){
            if(result != null) {
                showExpansionDetail((Expansion) result.getSerializableExtra(ExpansionDetailFragment.EXTRA_EXPANSION));
            } else {
                showConfigurationSelection();
            }
        } else if (fragment instanceof ExpansionDetailFragment) {
            if(result != null) {
                showExpansionAuth(result.getStringExtra(ExpansionsAuthFragment.EXTRA_INIT_URL),
                                  result.getStringExtra(ExpansionsAuthFragment.EXTRA_COMPLETE_URL));
            } else {
                showExpansionList();
            }
        }else if(fragment instanceof ExpansionsAuthFragment){
            showConfigurationSelection();
        } else if(fragment instanceof ConfigSelectionFragment){
            if(result == null){
                showExpansionList();
            } else{
                //todo show enabled configuration screen.
            }
        }
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

}
