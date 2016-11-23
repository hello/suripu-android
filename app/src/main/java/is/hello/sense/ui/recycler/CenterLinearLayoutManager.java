package is.hello.sense.ui.recycler;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;

import rx.functions.Func1;

/**
 * Smooth scrolls child views to center of parent container
 */

public class CenterLinearLayoutManager extends LinearLayoutManager {

    public CenterLinearLayoutManager(final Context context,
                                     final int orientation,
                                     final boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public void smoothScrollToPosition(final RecyclerView recyclerView,
                                       final RecyclerView.State state,
                                       final int position) {
        final CenterSmoothScroller scroller = new CenterSmoothScroller(recyclerView.getContext(),
                                                                       this::computeScrollVectorForPosition);
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }


    private static class CenterSmoothScroller extends LinearSmoothScroller{

        private final Func1<Integer,PointF> computeScrollFunc;

        CenterSmoothScroller(final Context context,
                                    final Func1<Integer,PointF> computeScrollFunc) {
            super(context);
            this.computeScrollFunc = computeScrollFunc;
        }

        @Override
        public PointF computeScrollVectorForPosition(final int targetPosition) {
            return this.computeScrollFunc.call(targetPosition);
        }

        @Override
        public int calculateDtToFit(final int viewStart,
                                    final int viewEnd,
                                    final int boxStart,
                                    final int boxEnd,
                                    final int snapPreference) {

            return boxStart - viewStart + ( (boxEnd - boxStart) - (viewEnd - viewStart) ) / 2;
        }
    }
}
