package is.hello.sense.mvp.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.ui.fragments.sounds.SleepSoundsFragment;

/**
 * This is an example of {@link SleepSoundsFragment} if it were to use {@link ViewPagerPresenterFragment}
 * instead
 */
//todo delete after testing.
public class TestViewPagerPresenterFragment extends ViewPagerPresenterFragment {

    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new SoundsViewPagerPresenterDelegate(getResources());
    }
}
