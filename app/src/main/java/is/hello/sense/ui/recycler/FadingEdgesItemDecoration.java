package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;

public class FadingEdgesItemDecoration extends RecyclerView.ItemDecoration {
    private final LinearLayoutManager layoutManager;
    private final GradientDrawable gradientDrawable;
    private final int edgeHeight;

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources) {
        this.layoutManager = layoutManager;
        this.edgeHeight = resources.getDimensionPixelSize(R.dimen.shadow_size);

        final @ColorInt int colors[] = {
                Color.TRANSPARENT,
                resources.getColor(R.color.shadow_end),
        };
        this.gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                                     colors);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int itemCount = parent.getAdapter().getItemCount();
        if (itemCount > parent.getChildCount()) {
            final int width = c.getWidth();
            final int height = c.getHeight();

            if (layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                gradientDrawable.setBounds(0, 0, width, edgeHeight);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                gradientDrawable.draw(c);
            }

            final int lastItem = itemCount - 1;
            if (layoutManager.findLastCompletelyVisibleItemPosition() < lastItem) {
                gradientDrawable.setBounds(0, height - edgeHeight, width, height);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                gradientDrawable.draw(c);
            }
        }
    }
}
