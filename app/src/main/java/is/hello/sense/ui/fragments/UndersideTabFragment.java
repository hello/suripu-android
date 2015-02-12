package is.hello.sense.ui.fragments;

import is.hello.sense.ui.common.InjectionFragment;

public abstract class UndersideTabFragment extends InjectionFragment {
    /**
     * Notifies the fragment that it is on screen, and may perform an update.
     */
    public final void tabSelected() {
        coordinator.postOnResume(() -> {
            onUpdate();
            onSwipeInteractionDidFinish();
        });
    }

    /**
     * Hook provided for subclasses to perform animations, etc
     * when they're guaranteed to be fully on-screen.
     */
    public abstract void onSwipeInteractionDidFinish();

    /**
     * Hook provided for subclasses to perform presenter
     * updates when its appropriate to do so.
     */
    public abstract void onUpdate();
}
