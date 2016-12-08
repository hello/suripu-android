package is.hello.sense.mvp.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

//todo move to is.hello.sense.flows.home.ui.fragments and replace SoundsFragment
public class SoundsPresenterFragment extends ViewPagerPresenterFragment {
    //region ViewPagerPresenterFragment
    @Override
    protected boolean setUpActionBar() {
        return false;
    }
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new SoundsViewPagerPresenterDelegate(getResources());
    }
    //endRegion
}