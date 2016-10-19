package is.hello.sense.flows.home.ui.fragments;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.graph.Scope;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.BacksideFragment;
import is.hello.sense.ui.recycler.PaddingItemDecoration;

public abstract class BacksideTabFragment<T extends PresenterView> extends PresenterFragment<T> {

    @Nullable
    private Rect contentInsets;

    /**
     * Returns the scope associated with the backside tab.
     */
    protected Scope getScope() {
        return (Scope) getActivity();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
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
     *
     * @return A {@code Rect} containing the insets.
     */
    protected Rect getContentInsets() {
        if (contentInsets == null) {
            final Resources resources = getResources();
            final int topInset = resources.getDimensionPixelSize(R.dimen.action_bar_height);
            final int bottomInset = resources.getDimensionPixelSize(R.dimen.sliding_layers_open_height);

            this.contentInsets = new Rect(0, topInset, 0, bottomInset);
        }

        return contentInsets;
    }

    /**
     * Indicates whether or not the tab wants to have its content automatically inset.
     * <p>
     * The insets will be applied after the fragment creates its view.
     *
     * @return true if the tab wants to have its content inset automatically; false otherwise.
     */
    protected boolean automaticallyApplyContentInsets() {
        return true;
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
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
