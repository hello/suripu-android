package is.hello.sense.ui.recycler;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class PaddingItemDecoration extends RecyclerView.ItemDecoration {
    protected final Rect insets;

    public PaddingItemDecoration(@NonNull Rect insets) {
        this.insets = insets;
    }

    public void setInsets(@NonNull Rect inRect) {
        insets.set(inRect);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int lastPosition = (parent.getAdapter().getItemCount() - 1);
        final int position = parent.getChildAdapterPosition(view);

        if (position == 0) {
            outRect.top += insets.top;
        }

         if (position == lastPosition) {
             outRect.bottom += insets.bottom;
         }

        outRect.left += insets.left;
        outRect.right += insets.right;
    }
}
