package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;

public abstract class FragmentNavigationActivity extends SenseActivity implements FragmentNavigation, FragmentManager.OnBackStackChangedListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_navigation);

        getFragmentManager().addOnBackStackChangedListener(this);

        //noinspection ConstantConditions
        getActionBar().setTitle(getDefaultTitle());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        if (getTopFragment() == null) {
            transaction.add(R.id.activity_fragment_navigation_container, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.activity_fragment_navigation_container, fragment, tag);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }

    @Override
    public void onBackStackChanged() {
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

    protected @Nullable Fragment getTopFragment() {
        return getFragmentManager().findFragmentById(R.id.activity_fragment_navigation_container);
    }

    protected abstract @StringRes int getDefaultTitle();
}
