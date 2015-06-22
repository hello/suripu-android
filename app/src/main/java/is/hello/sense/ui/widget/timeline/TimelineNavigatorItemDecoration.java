package is.hello.sense.ui.widget.timeline;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawing;

public class TimelineNavigatorItemDecoration extends RecyclerView.ItemDecoration {
    private static final float MIN_SCALE = 0.7f;
    private static final float MAX_SCALE = 1f;

    private final Drawable drawable;
    private final int dividerWidth;

    public TimelineNavigatorItemDecoration(@NonNull Resources resources) {
        this.drawable = ResourcesCompat.getDrawable(resources, R.drawable.graph_grid_fill_top_down, null);
        this.dividerWidth = resources.getDimensionPixelSize(R.dimen.divider_size);
    }


    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        int padding = parent.getMeasuredWidth() / 3;
        int position = parent.getChildAdapterPosition(child);
        if (position == 0) {
            outRect.right += padding;
        } else if (position == state.getItemCount() - 1) {
            outRect.left += padding;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        float recyclerCenter = parent.getWidth() / 2f;
        for (int i = 0, size = parent.getChildCount(); i < size; i++) {
            View child = parent.getChildAt(i);

            float childCenter = (child.getLeft() + child.getRight()) / 2f;
            float distanceAmount = 1f - Math.abs((childCenter - recyclerCenter) / recyclerCenter);

            float childScale = Drawing.interpolateFloats(distanceAmount, MIN_SCALE, MAX_SCALE);
            child.setScaleX(childScale);
            child.setScaleY(childScale);
        }
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        for (int i = 0, childCount = parent.getChildCount() - 1; i < childCount;i ++) {
            View child = parent.getChildAt(i);
            int left = child.getLeft(), top = child.getTop(), bottom = child.getBottom();
            drawable.setBounds(left, top, left + dividerWidth, bottom);
            drawable.draw(canvas);
        }
    }
}
