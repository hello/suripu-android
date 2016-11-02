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
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
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
    private static final String EXTRA_EXPANSION = ExpansionValuePickerActivity.class.getName() + "EXTRA_EXPANSION";
    private static final String EXTRA_IS_ENABLED = ExpansionValuePickerActivity.class.getName() + "EXTRA_IS_ENABLED";

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
            final Expansion expansion = (Expansion) intent.getSerializableExtra(EXTRA_EXPANSION);
            final ExpansionAlarm expansionAlarm = (ExpansionAlarm) intent.getSerializableExtra(EXTRA_EXPANSION_ALARM);
            if (expansionAlarm == null && expansion == null) {
                finish(); //todo handle better
                return;
            }
            if (expansionAlarm != null) {
                setTitle(expansionAlarm.getCategory().categoryDisplayString);
                showValuePicker(expansionAlarm.getId(),
                                expansionAlarm.getCategory(),
                                expansionAlarm.getExpansionRange(),
                                expansionAlarm.isEnabled());
            } else {
                setTitle(expansion.getCategory().categoryDisplayString);
                showValuePicker(expansion.getId(),
                                expansion.getCategory(),
                                expansion.getValueRange(),
                                intent.getBooleanExtra(EXTRA_IS_ENABLED, false));

            }
        }


    }

    public static Intent getIntent(@NonNull final Context context,
                                   @NonNull final ExpansionAlarm expansionAlarm) {
        return new Intent(context, ExpansionValuePickerActivity.class)
                .putExtra(EXTRA_EXPANSION_ALARM, expansionAlarm);
    }

    public static Intent getIntent(@NonNull final Context context,
                                   @NonNull final Expansion expansion,
                                   final boolean isEnabled) {
        return new Intent(context, ExpansionValuePickerActivity.class)
                .putExtra(EXTRA_EXPANSION, expansion)
                .putExtra(EXTRA_IS_ENABLED, isEnabled);
    }

    //region Router
    private void showValuePicker(final long expansionId,
                                 @NonNull final Category category,
                                 @Nullable final ExpansionValueRange valueRange,
                                 final boolean enabledForSmartAlarm) {
        pushFragment(ExpansionDetailFragment.newValuePickerInstance(expansionId,
                                                                    category,
                                                                    valueRange,
                                                                    enabledForSmartAlarm),
                     null, false);
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
                    // todo restore initial values by just popping fragment requires saving initial range state
                    popFragment(fragment, false);

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
