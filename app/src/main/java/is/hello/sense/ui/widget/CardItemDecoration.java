package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public final class CardItemDecoration extends RecyclerView.ItemDecoration {
    private final int outerHorizontal;
    private final int outerVertical;
    private final int inter;

    public @Nullable Rect contentInset;

    public CardItemDecoration(@NonNull Resources resources, boolean useCompact) {
        this.outerHorizontal = resources.getDimensionPixelSize(R.dimen.gap_card_horizontal);
        this.outerVertical = resources.getDimensionPixelSize(R.dimen.gap_card_vertical);

        if (useCompact) {
            this.inter = resources.getDimensionPixelSize(R.dimen.gap_card_inter_compact);
        } else {
            this.inter = resources.getDimensionPixelSize(R.dimen.gap_card_inter);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int lastPosition = (parent.getAdapter().getItemCount() - 1);
        int position = parent.getChildAdapterPosition(view);

        if (position == 0) {
            outRect.top = outerVertical;
        }

        if (position == lastPosition) {
            outRect.bottom = outerVertical;
        } else {
            outRect.bottom = inter;
        }

        outRect.left = outerHorizontal;
        outRect.right = outerHorizontal;

        if (contentInset != null) {
            outRect.left += contentInset.left;
            outRect.right += contentInset.right;

            if (position == 0) {
                outRect.top += contentInset.top;
            }

            if (position == lastPosition) {
                outRect.bottom += contentInset.bottom;
            }
        }
    }
}
