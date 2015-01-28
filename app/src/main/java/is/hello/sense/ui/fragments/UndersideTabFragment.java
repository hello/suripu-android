package is.hello.sense.ui.fragments;

import is.hello.sense.ui.common.InjectionFragment;

public abstract class UndersideTabFragment extends InjectionFragment {
    public final void pageSelected() {
        coordinator.postOnResume(this::onSwipeInteractionDidFinish);
    }
    public abstract void onSwipeInteractionDidFinish();
}
