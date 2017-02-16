package is.hello.sense.flows.home.ui.fragments;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.TrendsViewPagerPresenterDelegate;
import is.hello.sense.mvp.fragments.ViewPagerSenseViewFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class TrendsSenseViewFragment extends ViewPagerSenseViewFragment {

    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new TrendsViewPagerPresenterDelegate(getResources());
    }
}
