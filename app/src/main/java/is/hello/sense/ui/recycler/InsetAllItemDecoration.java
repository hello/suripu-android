package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.sense.util.Constants;

public class InsetAllItemDecoration extends RecyclerView.ItemDecoration {
    private final Rect insets;

    public InsetAllItemDecoration(@NonNull final Resources resources,
                                  @DimenRes final int allRes) {
        this(resources, allRes, allRes, allRes, allRes);
    }

    public InsetAllItemDecoration(@NonNull final Resources resources,
                                  @DimenRes final int horizontalRes,
                                  @DimenRes final int verticalRes) {
        this(resources, horizontalRes, verticalRes, horizontalRes, verticalRes);
    }

    public InsetAllItemDecoration(@NonNull final Resources resources,
                                  @DimenRes final int leftRes,
                                  @DimenRes final int topRes,
                                  @DimenRes final int rightRes,
                                  @DimenRes final int bottomRes) {
        this(new Rect(leftRes == Constants.NONE ? 0 : resources.getDimensionPixelSize(leftRes),
                      topRes == Constants.NONE ? 0 : resources.getDimensionPixelSize(topRes),
                      rightRes == Constants.NONE ? 0 : resources.getDimensionPixelSize(rightRes),
                      bottomRes == Constants.NONE ? 0 : resources.getDimensionPixelSize(bottomRes)));
    }

    public InsetAllItemDecoration(@NonNull final Rect insets) {
        this.insets = insets;
    }

    @Override
    public void getItemOffsets(final Rect outRect,
                               final View view,
                               final RecyclerView parent,
                               final RecyclerView.State state) {
        outRect.top += insets.top;
        outRect.left += insets.left;
        outRect.right += insets.right;
        outRect.bottom += insets.bottom;
    }
}
