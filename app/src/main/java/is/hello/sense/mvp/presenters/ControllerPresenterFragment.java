package is.hello.sense.mvp.presenters;

import android.support.annotation.CallSuper;

import is.hello.sense.flows.generic.ui.adapters.BaseFragmentPagerAdapter;
import is.hello.sense.mvp.view.PresenterView;

public abstract class ControllerPresenterFragment<T extends PresenterView> extends PresenterFragment<T>
        implements BaseFragmentPagerAdapter.Controller {
    private boolean isVisible = false;


    @CallSuper
    @Override
    public void setVisibleToUser(final boolean isVisible) {
        //debugLog("setVisibleToUser: [ " + isVisible + " ]"); //useful for debugging
        this.isVisible = isVisible;
    }

    @CallSuper
    @Override
    public boolean isVisibleToUser() {
        return isVisible;
    }


}
