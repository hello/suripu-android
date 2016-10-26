package is.hello.sense.ui.recycler;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.go99.Anime;
import is.hello.sense.ui.widget.CustomView;

/**
 * todo rename to match the custom adapter/view this was made for.
 *
 * This decoration is designed to be used with {@link CustomView} for correctly displaying the
 * middle/selected value and letting the view know what that position is.
 */
public class CustomItemDecoration extends RecyclerView.ItemDecoration {
    private static final float MIN_SCALE = 0.7f;
    private static final float MAX_SCALE = 1.5f;
    private static final float MIN_ALPHA = 0.2f;
    private static final float MAX_ALPHA = 1f;

    @Override
    public void getItemOffsets(final Rect outRect,
                               final View view,
                               final RecyclerView parent,
                               final RecyclerView.State state) {
        final int padding = parent.getMeasuredHeight() / 2;
        final int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.top += padding;
        } else if (position == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom += padding;
        }
    }

    @Override
    public void onDraw(final Canvas c,
                       final RecyclerView parent,
                       final RecyclerView.State state) {
        super.onDraw(c, parent, state);
        final float recyclerCenter = parent.getHeight() / 2;
        float greatestDistance = 0;
        int tempCenter = 0;
        for (int i = 0, size = parent.getChildCount(); i < size; i++) {
            final View child = parent.getChildAt(i);
            final float childCenter = (child.getTop() + child.getBottom()) / 2;
            final float distanceAmount = 1f - Math.abs((childCenter - recyclerCenter) / recyclerCenter);
            final float childScale = Anime.interpolateFloats(distanceAmount, MIN_SCALE, MAX_SCALE);
            final float childAlpha = Anime.interpolateFloats(distanceAmount, MIN_ALPHA, MAX_ALPHA);
            child.setScaleX(childScale);
            child.setScaleY(childScale);
            child.setAlpha(childAlpha);
            if (distanceAmount > greatestDistance) {
                greatestDistance = distanceAmount;
                tempCenter = parent.getChildAdapterPosition(child);
            }
        }

        if (parent instanceof CustomView) {
            ((CustomView) parent).setSelectedPosition(tempCenter);
        }
    }

}
