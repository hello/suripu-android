package is.hello.sense.ui.widget;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.R;

public final class CardItemDecoration extends RecyclerView.ItemDecoration {
    private final int outerHorizontal;
    private final int outerVertical;
    private final int inter;

    public CardItemDecoration(@NonNull Resources resources, boolean useCompact) {
        this.outerHorizontal = resources.getDimensionPixelSize(R.dimen.gap_card_sides);
        if (useCompact) {
            this.outerVertical = resources.getDimensionPixelSize(R.dimen.gap_card_header_footer_compact);
            this.inter = resources.getDimensionPixelSize(R.dimen.gap_card_spacing_compact);
        } else {
            this.outerVertical = resources.getDimensionPixelSize(R.dimen.gap_card_header_footer);
            this.inter = resources.getDimensionPixelSize(R.dimen.gap_card_spacing);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int lastPosition = (parent.getAdapter().getItemCount() - 1);
        int position = parent.getChildAdapterPosition(view);

        outRect.top = outerVertical;
        if (position == lastPosition) {
            outRect.bottom = outerVertical;
        } else {
            outRect.bottom = inter;
        }

        outRect.left = outerHorizontal;
        outRect.right = outerHorizontal;
    }
}
