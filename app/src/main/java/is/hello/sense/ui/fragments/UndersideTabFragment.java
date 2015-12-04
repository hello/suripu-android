package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.EnumSet;

import is.hello.sense.graph.Scope;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;

public abstract class UndersideTabFragment extends InjectionFragment {
    /**
     * Returns the scope associated with the underside tab.
     */
    protected Scope getScope() {
        return (Scope) getActivity();
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

    public void updateScrollEdgeShadows(@NonNull EnumSet<ScrollEdge> edges) {
        if (getUserVisibleHint()) {
            final UndersideFragment parent = (UndersideFragment) getParentFragment();
            parent.updateScrollEdgeShadows(edges);
        }
    }

    protected class EdgeShadowsScrollListener extends RecyclerView.OnScrollListener {
        private final LinearLayoutManager layoutManager;

        public EdgeShadowsScrollListener(@NonNull LinearLayoutManager layoutManager) {
            this.layoutManager = layoutManager;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            final EnumSet<ScrollEdge> edges = EnumSet.noneOf(ScrollEdge.class);

            if (layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                edges.add(ScrollEdge.TOP);
            }

            final int lastItem = recyclerView.getAdapter().getItemCount() - 1;
            if (layoutManager.findLastCompletelyVisibleItemPosition() < lastItem) {
                edges.add(ScrollEdge.BOTTOM);
            }

            updateScrollEdgeShadows(edges);
        }
    }
}
