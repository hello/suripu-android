package is.hello.sense.ui.fragments;

import is.hello.sense.graph.Scope;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;

public abstract class BacksideTabFragment extends InjectionFragment {
    /**
     * Returns the scope associated with the backside tab.
     */
    protected Scope getScope() {
        return (Scope) getActivity();
    }

    protected @OnboardingActivity.Flow int getOnboardingFlow() {
        final HomeActivity activity = (HomeActivity) getActivity();
        return activity != null
                ? activity.getOnboardingFlow()
                : OnboardingActivity.FLOW_NONE;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        final boolean wasVisibleToUser = getUserVisibleHint();
        super.setUserVisibleHint(isVisibleToUser);
        if (!wasVisibleToUser && isVisibleToUser) {
            stateSafeExecutor.execute(() -> {
                onUpdate();
                onSwipeInteractionDidFinish();
            });
        }
    }

    /**
     * Hook provided for subclasses to perform animations, etc
     * when they're guaranteed to be fully on-screen.
     */
    protected abstract void onSwipeInteractionDidFinish();

    /**
     * Hook provided for subclasses to perform presenter
     * updates when its appropriate to do so.
     */
    public abstract void onUpdate();
}
