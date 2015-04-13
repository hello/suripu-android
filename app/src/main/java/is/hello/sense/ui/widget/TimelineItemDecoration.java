package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class TimelineItemDecoration extends RecyclerView.ItemDecoration {
    private final Drawable drawable;
    private final int dividerWidth;

    public TimelineItemDecoration(@NonNull Drawable drawable, int dividerWidth) {
        this.drawable = drawable;
        this.dividerWidth = dividerWidth;
    }

    public TimelineItemDecoration(@NonNull Resources resources,
                                  @DrawableRes int drawableRes,
                                  @DimenRes int dividerWidthRes) {
        this(resources.getDrawable(drawableRes), resources.getDimensionPixelSize(dividerWidthRes));
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
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        for (int i = 0, childCount = parent.getChildCount() - 1; i < childCount;i ++) {
            View child = parent.getChildAt(i);
            int left = child.getLeft(), top = child.getTop(), bottom = child.getBottom();
            drawable.setBounds(left, top, left + dividerWidth, bottom);
            drawable.draw(canvas);
        }
    }
}
