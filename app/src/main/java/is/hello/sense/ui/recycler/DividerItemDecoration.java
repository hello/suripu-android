package is.hello.sense.ui.recycler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint = new Paint();
    private final int height;
    private final Rect inset;

    public static DividerItemDecoration getLeftInset(@NonNull final Context context,
                                                     final int offsetPx) {
        final Rect dividerInset = new Rect(offsetPx, 0, 0, 0);
        return new DividerItemDecoration(context, dividerInset);
    }

    public DividerItemDecoration(@NonNull final Context context) {
        this(context, new Rect());
    }

    /**
     * @param context used to fetch divider size and color
     * @param inset applies inset to divider
     */
    public DividerItemDecoration(@NonNull final Context context,
                                 @NonNull final Rect inset) {
        this.height = context.getResources().getDimensionPixelSize(R.dimen.divider_size);
        paint.setColor(ContextCompat.getColor(context, R.color.divider));
        this.inset = inset;
    }

    @Override
    public void onDrawOver(final Canvas c,
                           final RecyclerView parent,
                           final RecyclerView.State state) {
        final int lastItem = state.getItemCount() - 2;
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            final View child = parent.getChildAt(i);
            final int position = parent.getChildAdapterPosition(child);
            if (position <= lastItem) {
                final int bottom = child.getBottom();
                c.drawRect(child.getLeft() + inset.left,
                           bottom - height + inset.top,
                           child.getRight() + inset.right,
                           bottom + inset.bottom,
                           paint);
            }
        }
    }
}
