package is.hello.sense.ui.recycler;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;

import java.lang.ref.WeakReference;

import is.hello.sense.functional.Functions;
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

        private final WeakReference<Func1<Integer,PointF>> computeScrollFunc;

        CenterSmoothScroller(final Context context,
                             final Func1<Integer,PointF> computeScrollFunc) {
            super(context);
            this.computeScrollFunc = new WeakReference<>(computeScrollFunc);
        }

        @Override
        public PointF computeScrollVectorForPosition(final int targetPosition) {
            final Func1<Integer,PointF> func1 = Functions.extract(this.computeScrollFunc);
            if(func1 != null) {
                return func1.call(targetPosition);
            } else {
                return null;
            }
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
