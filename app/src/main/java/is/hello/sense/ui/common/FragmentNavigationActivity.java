package is.hello.sense.ui.common;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.Logger;

public class FragmentNavigationActivity extends SenseActivity
        implements FragmentNavigation, FragmentManager.OnBackStackChangedListener {
    public static final String EXTRA_DEFAULT_TITLE = FragmentNavigationActivity.class.getName() + ".EXTRA_DEFAULT_TITLE";
    public static final String EXTRA_FRAGMENT_CLASS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_CLASS";
    public static final String EXTRA_FRAGMENT_ARGUMENTS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_ARGUMENTS";
    public static final String EXTRA_WINDOW_COLOR = FragmentNavigationActivity.class.getName() + ".EXTRA_WINDOW_COLOR";
    /**
     * @see is.hello.sense.ui.common.FragmentNavigationActivity.Builder#setOrientation(int) for more info.
     */
    @Deprecated
    public static final String EXTRA_ORIENTATION = FragmentNavigationActivity.class.getName() + ".EXTRA_ORIENTATION";

    private FragmentNavigationDelegate navigationDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_navigation);

        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                               R.id.activity_fragment_navigation_container,
                                               null);

        getFragmentManager().addOnBackStackChangedListener(this);

        final Intent intent = getIntent();
        if (savedInstanceState == null) {
            //noinspection ConstantConditions
            getActionBar().setTitle(getDefaultTitle());

            if (intent.hasExtra(EXTRA_FRAGMENT_CLASS)) {
                try {
                    final String className = intent.getStringExtra(EXTRA_FRAGMENT_CLASS);
                    //noinspection unchecked
                    final Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) Class.forName(className);
                    final Fragment fragment = fragmentClass.newInstance();
                    fragment.setArguments(intent.getParcelableExtra(EXTRA_FRAGMENT_ARGUMENTS));
                    pushFragment(fragment, getDefaultTitle(), false);
                } catch (Exception e) {
                    Logger.warn(getClass().getSimpleName(), "Could not create fragment", e);
                }
            }
        } else {
            final String title = savedInstanceState.getString("title");
            //noinspection ConstantConditions
            getActionBar().setTitle(title);

            navigationDelegate.onRestoreInstanceState(savedInstanceState);
        }

        if (intent.hasExtra(EXTRA_WINDOW_COLOR)) {
            final int windowColor = intent.getIntExtra(EXTRA_WINDOW_COLOR, Color.TRANSPARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(windowColor));
        }

        // This should be removed when all on-boarding components
        // are migrated to the model-view-presenter pattern.
        if (intent.hasExtra(EXTRA_ORIENTATION)) {
            final int orientation = intent.getIntExtra(EXTRA_ORIENTATION,
                                                       ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            //noinspection ResourceType
            setRequestedOrientation(orientation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home &&
                getFragmentManager().getBackStackEntryCount() > 0) {
            final View focusView = getCurrentFocus();
            if (focusView != null && focusView instanceof EditText) {
                final InputMethodManager inputMethodManager =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }
            getFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null && actionBar.getTitle() != null) {
            outState.putString("title", actionBar.getTitle().toString());
        }

        navigationDelegate.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        navigationDelegate.onDestroy();
        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackPressed() {
        final Fragment topFragment = getTopFragment();
        if (topFragment instanceof OnBackPressedInterceptor) {
            if (((OnBackPressedInterceptor) topFragment).onInterceptBackPressed(super::onBackPressed)) {
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void pushFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                              @Nullable String title,
                                              boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    public void overlayFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                                 @Nullable String title,
                                                 boolean wantsBackStackEntry) {
        final String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.activity_fragment_navigation_container, fragment, tag);
        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(tag);
        }
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void popFragment(@NonNull Fragment fragment, boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull Fragment fragment,
                             int responseCode,
                             @Nullable Intent result) {
        Logger.debug(getClass().getSimpleName(),
                     "flowFinished(" + fragment + ", " + responseCode + ", " + result + ")");

        setResult(responseCode, result);
        finish();
    }

    @Override
    public void onBackStackChanged() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            final FragmentManager fragmentManager = getFragmentManager();
            final int entryCount = fragmentManager.getBackStackEntryCount();
            if (entryCount > 0) {
                final FragmentManager.BackStackEntry entry =
                        fragmentManager.getBackStackEntryAt(entryCount - 1);
                actionBar.setTitle(entry.getBreadCrumbTitle());
            } else {
                actionBar.setTitle(getDefaultTitle());
            }
        }
    }

    @Override
    public @Nullable Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }

    protected @Nullable String getDefaultTitle() {
        return getIntent().getStringExtra(EXTRA_DEFAULT_TITLE);
    }


    public static class Builder {
        private final Activity from;
        private final Intent intent;

        public Builder(@NonNull Activity from) {
            this(from, FragmentNavigationActivity.class);
        }

        public Builder(@NonNull Activity from,
                       @NonNull Class<? extends FragmentNavigationActivity> clazz) {
            this.from = from;
            this.intent = new Intent(from, clazz);
        }

        public Builder setDefaultTitle(@Nullable String title) {
            intent.putExtra(EXTRA_DEFAULT_TITLE, title);
            return this;
        }

        public Builder setDefaultTitle(@StringRes int titleRes) {
            return setDefaultTitle(from.getString(titleRes));
        }

        public Builder setFragmentClass(@NonNull Class<? extends Fragment> clazz) {
            intent.putExtra(EXTRA_FRAGMENT_CLASS, clazz.getName());
            return this;
        }

        public Builder setArguments(@Nullable Bundle arguments) {
            intent.putExtra(EXTRA_FRAGMENT_ARGUMENTS, arguments);
            return this;
        }

        public Builder setWindowBackgroundColor(@ColorInt int backgroundColor) {
            intent.putExtra(EXTRA_WINDOW_COLOR, backgroundColor);
            return this;
        }

        /**
         * Specifies the orientation to request on the navigation fragment.
         *
         * @deprecated There is no replacement. This method is provided as
         *             a transitional API for on-boarding components starting
         *             the process of being de-coupled and refactored into MVP.
         * @param orientation   The {@link Activity} orientation to request.
         * @return The builder.
         */
        @Deprecated
        public Builder setOrientation(int orientation) {
            intent.putExtra(EXTRA_ORIENTATION, orientation);
            return this;
        }

        public Intent toIntent() {
            return intent;
        }
    }
}
