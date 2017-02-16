package is.hello.sense.mvp.fragments;

import android.support.annotation.CallSuper;

import is.hello.sense.mvp.view.SenseView;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;

public abstract class ControllerSenseViewFragment<T extends SenseView> extends SenseViewFragment<T>
        implements StaticFragmentAdapter.Controller {
    private boolean isVisible = false;

    @CallSuper
    @Override
    public void setVisibleToUser(final boolean isVisible) {
       // debugLog("setVisibleToUser: [ "+isVisible+" ]"); //useful for debugging
        this.isVisible = isVisible;
    }

    @CallSuper
    @Override
    public boolean isVisibleToUser() {
        return isVisible;
    }
}
