package is.hello.sense.ui.recycler;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ContentInsetItemDecoration extends RecyclerView.ItemDecoration {
    private final Rect insets;

    public ContentInsetItemDecoration(@NonNull Rect insets) {
        this.insets = insets;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int lastPosition = (parent.getAdapter().getItemCount() - 1);
        int position = parent.getChildAdapterPosition(view);

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
