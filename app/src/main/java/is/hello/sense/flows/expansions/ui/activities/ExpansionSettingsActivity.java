package is.hello.sense.flows.expansions.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
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

    private static final String EXTRA_EXPANSION_DETAIL_ID = ExpansionSettingsActivity.class.getName() + "EXTRA_EXPANSION_DETAIL_ID";

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

            if(wantsExpansionDetailThenStartPickerActivity()){
                showExpansionDetail(getIntent().getLongExtra(EXTRA_EXPANSION_DETAIL_ID, Expansion.NO_ID));
            } else {
                showExpansionList();
            }
        }


    }

    public static Intent getExpansionDetailIntent(@NonNull final Context context, final long expansionId){
        return new Intent(context, ExpansionSettingsActivity.class)
                .putExtra(EXTRA_EXPANSION_DETAIL_ID, expansionId);
    }

    private boolean wantsExpansionDetailThenStartPickerActivity() {
        return getIntent().hasExtra(EXTRA_EXPANSION_DETAIL_ID);
    }

    //region Router

    private void showExpansionList() {
        pushFragment(new ExpansionListFragment(), null, false);
    }

    private void showExpansionDetail(final long expansionId) {
        //todo setting to false here because it will overriding onBackPressed to return to ExpansionList in different PR
        pushFragment(ExpansionDetailFragment.newInstance(expansionId), null, false);
    }

    private void showExpansionAuth() {
        pushFragment(new ExpansionsAuthFragment(), null, true);
    }

    public void showConfigurationSelection() {
        pushFragment(new ConfigSelectionFragment(), null, false);
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
            popFragment(fragment, false);
        } else if (fragment instanceof ExpansionListFragment
                && result != null
                && result.hasExtra(ExpansionListFragment.EXPANSION_ID_KEY)) {
            showExpansionDetail(result.getLongExtra(ExpansionListFragment.EXPANSION_ID_KEY, Expansion.NO_ID));
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
            if (result != null
                    && result.hasExtra(ConfigSelectionFragment.EXPANSION_ID_KEY)
                    && result.hasExtra(ConfigSelectionFragment.EXPANSION_CATEGORY)) {
                final long expansionId = result.getLongExtra(ConfigSelectionFragment.EXPANSION_ID_KEY, Expansion.NO_ID);
                if(wantsExpansionDetailThenStartPickerActivity()){
                    final Category category = (Category) result.getSerializableExtra(ConfigSelectionFragment.EXPANSION_CATEGORY);
                    showExpansionPickerActivity(expansionId, category);
                } else {
                    showExpansionDetail(expansionId);
                }
            } else {
                if(wantsExpansionDetailThenStartPickerActivity()) {
                    setResult(RESULT_CANCELED); //todo handle better
                    finish();
                } else {
                    showExpansionList();
                }
            }
        }
    }

    private void showExpansionPickerActivity(final long expansionId, @NonNull final Category category) {
        final Intent intent = ExpansionValuePickerActivity.getIntent(this, expansionId, category, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final Fragment fragment = getTopFragment();
        if (fragment instanceof ExpansionsAuthFragment && fragment.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (!(fragment instanceof OnBackPressedInterceptor) ||
                !((OnBackPressedInterceptor) fragment).onInterceptBackPressed(stateSafeExecutor.bind(super::onBackPressed))) {
                    stateSafeExecutor.execute(super::onBackPressed);
                }
    }


}
