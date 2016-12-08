package is.hello.sense.mvp.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.flows.home.util.TrendsViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class TrendsPresenterFragment extends ViewPagerPresenterFragment {
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new TrendsViewPagerPresenterDelegate(getResources());
    }
}
