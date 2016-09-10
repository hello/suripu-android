package is.hello.sense.mvp.presenters.home;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.graph.Scope;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.BacksideFragment;
import is.hello.sense.ui.recycler.PaddingItemDecoration;

public abstract class BacksideTabFragment<T extends PresenterView> extends PresenterFragment<T> {
    /**
     * Taken from {@code SwipeRefreshLayout} private constant. The default amount of space
     * a {@code SwipeRefreshLayout} places before the refresh indicator when indicator comes
     * to rest after the user finishes swiping.
     */
    private static final int DEFAULT_REFRESH_CIRCLE_TARGET = 64;

    @Nullable
    private Rect contentInsets;

    /**
     * Returns the scope associated with the backside tab.
     */
    protected Scope getScope() {
        return (Scope) getActivity();
    }

    @OnboardingActivity.Flow
    protected int getOnboardingFlow() {
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
     * Retrieves the {@link is.hello.sense.ui.fragments.BacksideFragment} this tab fragment is contained in.
     *
     * @return The fragment if the tab is attached; null otherwise.
     */
    protected is.hello.sense.ui.fragments.BacksideFragment getBacksideFragment() {
        return (BacksideFragment) getParentFragment();
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

    /**
     * Applies content insets to a recycler view that is in a non-inset container.
     *
     * @param recyclerView The recycler view to inset. Uses {@link PaddingItemDecoration}.
     * @throws IllegalStateException if {@link #automaticallyApplyContentInsets()} returns true.
     */
    protected void insetRecyclerView(@NonNull RecyclerView recyclerView) {
        if (automaticallyApplyContentInsets()) {
            throw new IllegalStateException("insetRecyclerView not allowed with" +
                                                    "automaticallyApplyContentInsets returning true");
        }

        final Rect insets = getContentInsets();
        recyclerView.addItemDecoration(new PaddingItemDecoration(insets), 0);
    }

    protected void insetSwipeRefreshLayout(@NonNull SwipeRefreshLayout swipeRefreshLayout) {
        if (automaticallyApplyContentInsets()) {
            throw new IllegalStateException("insetSwipeRefreshLayout not allowed with" +
                                                    "automaticallyApplyContentInsets returning true");
        }

        final Rect contentInsets = getContentInsets();
        swipeRefreshLayout.setDistanceToTriggerSync(contentInsets.top);

        final float density = getResources().getDisplayMetrics().density;
        final int defaultEnd = Math.round(DEFAULT_REFRESH_CIRCLE_TARGET * density);
        swipeRefreshLayout.setProgressViewEndTarget(false, defaultEnd + contentInsets.top);
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
