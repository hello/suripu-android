package is.hello.sense.debug;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.util.NotTested;
import is.hello.sense.util.StateSafeExecutor;

//todo delete this class when done testing
public class TestActivity extends Activity
        implements FragmentNavigation {
    protected boolean isResumed = false;
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(() -> isResumed);
    private FragmentNavigationDelegate navigationDelegate;
 
 
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);

        pushFragment(new SoundsPresenterFragment(), null, true);


    }

    @Override
    protected void onPause() {
        super.onPause();

        this.isResumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.isResumed = true;
        stateSafeExecutor.executePendingForResume();
    }
    @NotTested
    @Override
    public final void pushFragment(@NonNull final Fragment fragment,
                                   @Nullable final String title,
                                   final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @NotTested
    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                                    @Nullable final String title,
                                                    final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @NotTested
    @Override
    public final void popFragment(@NonNull final Fragment fragment,
                                  final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @NotTested
    @Override
    public final void flowFinished(@NonNull final Fragment fragment,
                                   final int responseCode,
                                   @Nullable final Intent result) {
    }

    @NotTested
    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }
    //endregion
}
