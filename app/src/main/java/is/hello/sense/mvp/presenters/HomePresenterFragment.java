package is.hello.sense.mvp.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.HomeViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

//todo move to is.hello.sense.flows.home.ui.fragments and replace HomeFragment
public class HomePresenterFragment extends ViewPagerPresenterFragment {
    //region ViewPagerPresenterFragment
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new HomeViewPagerPresenterDelegate(getResources());
    }
    //endRegion
}