package is.hello.sense.debug;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.mvp.presenters.TestViewPagerPresenterFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.NotTested;

//todo delete this class when done testing
public class TestActivity extends InjectionActivity
        implements FragmentNavigation {
    private FragmentNavigationDelegate navigationDelegate;

    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);

        pushFragment(new TestViewPagerPresenterFragment(), null, true);


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
