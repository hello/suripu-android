package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint = new Paint();
    private final int height;

    public DividerItemDecoration(@NonNull Resources resources) {
        this.height = resources.getDimensionPixelSize(R.dimen.divider_size);
        paint.setColor(resources.getColor(R.color.border));
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int lastItem = state.getItemCount() - 2;
        final int position = parent.getChildAdapterPosition(view);
        if (position <= lastItem) {
            outRect.bottom += height;
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int lastItem = state.getItemCount() - 2;
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            final View child = parent.getChildAt(i);
            final int position = parent.getChildAdapterPosition(child);
            if (position <= lastItem) {
                final int bottom = child.getBottom();
                c.drawRect(child.getLeft(), bottom - height,
                           child.getRight(), bottom,
                           paint);
            }
        }
    }
}
