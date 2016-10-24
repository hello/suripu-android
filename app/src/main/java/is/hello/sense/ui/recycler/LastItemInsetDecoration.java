package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public final class LastItemInsetDecoration extends RecyclerView.ItemDecoration {
    private final int inset;

    public LastItemInsetDecoration(@NonNull final Resources resources) {
        this.inset = resources.getDimensionPixelSize(R.dimen.x1);
    }

    @Override
    public void getItemOffsets(final Rect outRect,
                               final View view,
                               final RecyclerView parent,
                               final RecyclerView.State state) {
        final int lastPosition = (parent.getAdapter().getItemCount() - 1);
        final int position = parent.getChildAdapterPosition(view);


        if (position == lastPosition) {
            outRect.bottom = inset;
        }
        if (position == 0) {
            outRect.top = inset;
        }

        outRect.left = inset;
        outRect.right = inset;

    }
}
