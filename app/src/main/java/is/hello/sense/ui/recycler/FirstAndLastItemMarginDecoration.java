package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public class FirstAndLastItemMarginDecoration extends RecyclerView.ItemDecoration {

    private final int margin;

    public FirstAndLastItemMarginDecoration(@NonNull final Resources resources) {
        this.margin = resources.getDimensionPixelSize(R.dimen.x1);
    }

    @Override
    public void getItemOffsets(final Rect outRect,
                               final View view,
                               final RecyclerView parent,
                               final RecyclerView.State state) {
        final int lastPosition = (parent.getAdapter().getItemCount() - 1);
        final int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.top = margin;
        } else if (position == lastPosition) {
            outRect.bottom = margin;
        }
        outRect.left = margin;
        outRect.right = margin;

    }
}
