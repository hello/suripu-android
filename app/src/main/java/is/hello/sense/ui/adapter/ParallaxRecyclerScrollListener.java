package is.hello.sense.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Provides parallax offset calculation for subclasses of {@link ParallaxRecyclerViewHolder}.
 * Assumes that all view holders in the recycler view are subclasses.
 */
public class ParallaxRecyclerScrollListener extends RecyclerView.OnScrollListener {
    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        final int recyclerCenter = recyclerView.getMeasuredHeight() / 2;
        for (int i = 0, count = recyclerView.getChildCount(); i < count; i++) {
            final View childView = recyclerView.getChildAt(i);
            final int childViewCenter = (childView.getTop() + childView.getBottom()) / 2;
            final ParallaxRecyclerViewHolder viewHolder =
                    (ParallaxRecyclerViewHolder) recyclerView.getChildViewHolder(childView);

            // For the purpose of calculating a parallax offset, the recycler view is split
            // into two segments: the area above its center, and the area below. The parallax
            // percentage is then calculated as the distance from this center relative to the
            // view holder.
            //
            // +------- (top, -1.0f)
            // |
            // | <--- (top mid-point, -0.5f)
            // |
            // +------- (center, 0.0f)
            // |
            // | <--- (bottom mid-point)
            // |
            // +------- (bottom, 1.0f)
            final float rawPercent = (childViewCenter - recyclerCenter) / (float) recyclerCenter;
            final float percent = Math.min(1f, Math.max(-1f, rawPercent));
            viewHolder.setParallaxPercent(percent);
        }
    }
}
