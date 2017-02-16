package is.hello.sense.flows.home.ui.fragments;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.TrendsViewPagerPresenterDelegate;
import is.hello.sense.mvp.fragments.ViewPagerPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class TrendsPresenterFragment extends ViewPagerPresenterFragment {

    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new TrendsViewPagerPresenterDelegate(getResources());
    }
}
