package is.hello.sense.mvp.presenters;

import android.os.Bundle;
import android.support.annotation.CallSuper;

import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;

public abstract class ControllerPresenterFragment<T extends PresenterView> extends PresenterFragment<T>
        implements StaticFragmentAdapter.Controller {
    private static final String KEY_IS_VISIBLE = ControllerPresenterFragment.class.getSimpleName() + ".KEY_IS_VISIBLE";
    private boolean isVisible = false;
    private boolean restoreStateOnStart = false;


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

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // SetUserVisible should only be called when the presenterview is initialized.
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.restoreStateOnStart = savedInstanceState.getBoolean(KEY_IS_VISIBLE, false);
        }
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_VISIBLE, isVisible);
    }

    @CallSuper
    @Override
    public void onStart() {
        super.onStart();
        if (this.restoreStateOnStart) {
            this.restoreStateOnStart = false;
            setVisibleToUser(true);
        }
    }

}
