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
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.flows.expansions.modules.ExpansionSettingsModule;
import is.hello.sense.flows.expansions.ui.fragments.ConfigSelectionFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionDetailFragment;
import is.hello.sense.flows.expansions.ui.fragments.ExpansionsAuthFragment;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.OnBackPressedInterceptor;

public class ExpansionValuePickerActivity extends ScopedInjectionActivity
        implements FragmentNavigation {

    public static final String EXTRA_EXPANSION_ALARM = ExpansionValuePickerActivity.class.getName() + "EXTRA_EXPANSION_ALARM";

    private static final String EXTRA_EXPANSION_DETAIL_ID = ExpansionValuePickerActivity.class.getName() + "EXTRA_EXPANSION_DETAIL_ID";
    private static final String EXTRA_EXPANSION_CATEGORY = ExpansionValuePickerActivity.class.getName() + "EXTRA_EXPANSION_CATEGORY";
    private static final String EXTRA_EXPANSION_VALUE_RANGE = ExpansionValuePickerActivity.class.getName() + "EXTRA_EXPANSION_VALUE_RANGE";

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
            final Intent intent = getIntent();

            final Category category = (Category) intent.getSerializableExtra(EXTRA_EXPANSION_CATEGORY);
            if(category != null) {
                setTitle(category.categoryDisplayString);
                showValuePicker(intent.getLongExtra(EXTRA_EXPANSION_DETAIL_ID, Expansion.NO_ID),
                                category,
                                (ExpansionValueRange) intent.getSerializableExtra(EXTRA_EXPANSION_VALUE_RANGE));
            } else {
                finish(); //todo handle better
            }
        }


    }

    public static Intent getIntent(@NonNull final Context context,
                                   final long expansionId,
                                   @NonNull final Category expansionCategory,
                                   @Nullable final ExpansionValueRange expansionValueRange){
        return new Intent(context, ExpansionValuePickerActivity.class)
                .putExtra(EXTRA_EXPANSION_DETAIL_ID, expansionId)
                .putExtra(EXTRA_EXPANSION_CATEGORY, expansionCategory)
                .putExtra(EXTRA_EXPANSION_VALUE_RANGE, expansionValueRange);
    }

    //region Router
    //todo use category to decide how many pickers to inflate
    private void showValuePicker(final long expansionId,
                                 @NonNull final Category category,
                                 @Nullable final ExpansionValueRange valueRange) {
        pushFragment(ExpansionDetailFragment.newValuePickerInstance(expansionId,
                                                                    category,
                                                                    valueRange),
                     null, false);
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
        } else {
            if (fragment instanceof ExpansionDetailFragment) {
                //todo handle RESULT_CONNECT_PRESSED or cancel flow if not authenticated?
                if (responseCode == ExpansionDetailFragment.RESULT_CONFIGURE_PRESSED) {
                    showConfigurationSelection();
                } else {
                    setResult(RESULT_OK, result);
                    finish();
                }
            } else if (fragment instanceof ConfigSelectionFragment) {
                if (result != null
                        && result.hasExtra(ConfigSelectionFragment.EXPANSION_ID_KEY)
                        && result.hasExtra(ConfigSelectionFragment.EXPANSION_CATEGORY)) {
                    final long expansionId = result.getLongExtra(ConfigSelectionFragment.EXPANSION_ID_KEY, Expansion.NO_ID);
                        final Category category = (Category) result.getSerializableExtra(ConfigSelectionFragment.EXPANSION_CATEGORY);
                        showValuePicker(expansionId, category, null);
                    }
                } else {
                    setResult(RESULT_CANCELED);
                    finish(); //todo handle better
                }
        }
    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
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

    @Override
    public void onBackPressed() {
        final Fragment fragment = getTopFragment();
        if (!(fragment instanceof OnBackPressedInterceptor) ||
                !((OnBackPressedInterceptor) fragment).onInterceptBackPressed(stateSafeExecutor.bind(super::onBackPressed))) {
                    stateSafeExecutor.execute(super::onBackPressed);
                }
    }


}
