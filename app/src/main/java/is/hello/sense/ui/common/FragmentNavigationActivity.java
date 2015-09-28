package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.Logger;

public class FragmentNavigationActivity extends SenseActivity implements FragmentNavigation, FragmentManager.OnBackStackChangedListener {
    public static final String EXTRA_DEFAULT_TITLE = FragmentNavigationActivity.class.getName() + ".EXTRA_DEFAULT_TITLE";
    public static final String EXTRA_FRAGMENT_CLASS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_CLASS";
    public static final String EXTRA_FRAGMENT_ARGUMENTS = FragmentNavigationActivity.class.getName() + ".EXTRA_FRAGMENT_ARGUMENTS";

    private boolean wantsTitleUpdates = true;

    public static Bundle getArguments(@NonNull String defaultTitle,
                                      @NonNull Class<? extends Fragment> fragmentClass,
                                      @Nullable Bundle fragmentArguments) {
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_DEFAULT_TITLE, defaultTitle);
        arguments.putString(EXTRA_FRAGMENT_CLASS, fragmentClass.getName());
        arguments.putParcelable(EXTRA_FRAGMENT_ARGUMENTS, fragmentArguments);
        return arguments;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_navigation);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (savedInstanceState == null) {
            //noinspection ConstantConditions
            getActionBar().setTitle(getDefaultTitle());

            if (getIntent().hasExtra(EXTRA_FRAGMENT_CLASS)) {
                try {
                    final String className = getIntent().getStringExtra(EXTRA_FRAGMENT_CLASS);
                    //noinspection unchecked
                    final Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) Class.forName(className);
                    final Fragment fragment = fragmentClass.newInstance();
                    fragment.setArguments(getIntent().getParcelableExtra(EXTRA_FRAGMENT_ARGUMENTS));
                    pushFragment(fragment, getDefaultTitle(), false);
                } catch (Exception e) {
                    Logger.warn(getClass().getSimpleName(), "Could not create fragment", e);
                }
            }
        } else {
            final String title = savedInstanceState.getString("title");
            //noinspection ConstantConditions
            getActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home &&
                getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //noinspection ConstantConditions
        outState.putString("title", getActionBar().getTitle().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackPressed() {
        final Fragment topFragment = getTopFragment();
        if (topFragment instanceof BackInterceptingFragment) {
            if (((BackInterceptingFragment) topFragment).onInterceptBack(super::onBackPressed)) {
                return;
            }
        }

        super.onBackPressed();
    }

    protected FragmentTransaction createTransaction(@NonNull Fragment fragment,
                                                    @Nullable String title,
                                                    boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final String tag = fragment.getClass().getSimpleName();
        if (getTopFragment() == null) {
            transaction.add(R.id.activity_fragment_navigation_container, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.activity_fragment_navigation_container, fragment, tag);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(tag);
        }

        return transaction;
    }

    @Override
    public void pushFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = createTransaction(fragment, title,
                                                                  wantsBackStackEntry);
        transaction.commit();
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                              @Nullable String title,
                                              boolean wantsBackStackEntry) {
        final FragmentTransaction transaction = createTransaction(fragment, title,
                                                                  wantsBackStackEntry);
        transaction.commitAllowingStateLoss();
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
        String tag = fragment.getClass().getSimpleName();
        if (immediate) {
            getFragmentManager().popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            getFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onBackStackChanged() {
        if (getWantsTitleUpdates()) {
            int entryCount = getFragmentManager().getBackStackEntryCount();
            if (entryCount > 0) {
                FragmentManager.BackStackEntry entry = getFragmentManager().getBackStackEntryAt(entryCount - 1);
                //noinspection ConstantConditions
                getActionBar().setTitle(entry.getBreadCrumbTitle());
            } else {
                //noinspection ConstantConditions
                getActionBar().setTitle(getDefaultTitle());
            }
        }
    }

    @Override
    public @Nullable Fragment getTopFragment() {
        return getFragmentManager().findFragmentById(R.id.activity_fragment_navigation_container);
    }

    protected @Nullable String getDefaultTitle() {
        return getIntent().getStringExtra(EXTRA_DEFAULT_TITLE);
    }

    public boolean getWantsTitleUpdates() {
        return wantsTitleUpdates;
    }

    public void setWantsTitleUpdates(boolean wantsTitleUpdates) {
        this.wantsTitleUpdates = wantsTitleUpdates;
    }
}
