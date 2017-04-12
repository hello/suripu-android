package is.hello.sense.mvp.presenters;


import android.support.annotation.CallSuper;

import javax.inject.Inject;

import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.mvp.view.PresenterView;

public abstract class BaseHomeFragment<T extends PresenterView> extends ControllerPresenterFragment<T> {
    @Inject
    NightModeInteractor nightModeInteractor;

    @CallSuper
    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        if (isVisible) {
            nightModeInteractor.updateIfAuto();
        }
    }
}
