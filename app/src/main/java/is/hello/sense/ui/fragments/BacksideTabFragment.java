package is.hello.sense.ui.fragments;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import is.hello.sense.R;
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


    //region Content Insets

    /**
     * Calculates the insets that should be applied to fragment's
     * content views to appear visually correct.
     * @return  A {@code Rect} containing the insets.
     */
    protected Rect getContentInsets() {
        final Resources resources = getResources();
        final int topInset = resources.getDimensionPixelSize(R.dimen.action_bar_height);
        final int bottomInset = resources.getDimensionPixelSize(R.dimen.sliding_layers_open_height);
        return new Rect(0, topInset, 0, bottomInset);
    }

    /**
     * Indicates whether or not the tab wants to have its content automatically inset.
     * <p>
     * The insets will be applied after the fragment creates its view.
     * @return true if the tab wants to have its content inset automatically; false otherwise.
     */
    protected boolean automaticallyApplyContentInsets() {
        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (automaticallyApplyContentInsets()) {
            final Rect contentInsets = getContentInsets();

            final int newTopPadding = view.getPaddingTop() + contentInsets.top;
            final int newBottomPadding = view.getPaddingBottom() + contentInsets.bottom;
            if (view.isPaddingRelative()) {
                view.setPaddingRelative(view.getPaddingStart(),
                                        newTopPadding,
                                        view.getPaddingEnd(),
                                        newBottomPadding);
            } else {
                view.setPadding(view.getPaddingLeft(),
                                newTopPadding,
                                view.getPaddingRight(),
                                newBottomPadding);
            }
        }
    }

    //endregion
}
