package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import is.hello.sense.R;

public class FadingEdgesItemDecoration extends RecyclerView.ItemDecoration {
    public static final int EDGE_TOP = (1 << 1);
    public static final int EDGE_BOTTOM = (1 << 2);

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @IntDef(flag = true, value = {
            EDGE_TOP,
            EDGE_BOTTOM,
    })
    public @interface Edges {}

    private final LinearLayoutManager layoutManager;
    private final GradientDrawable gradientDrawable;
    private final int edgeHeight;
    private final @Edges int edges;

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources,
                                     @Edges int edges) {
        this.layoutManager = layoutManager;
        this.edgeHeight = resources.getDimensionPixelSize(R.dimen.shadow_size);
        this.edges = edges;

        final @ColorInt int colors[] = {
                Color.TRANSPARENT,
                resources.getColor(R.color.shadow_end),
        };
        this.gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                                     colors);
    }

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources) {
        this(layoutManager, resources, EDGE_TOP | EDGE_BOTTOM);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final boolean wantsTopEdge = ((edges & EDGE_TOP) == EDGE_TOP);
        final boolean wantsBottomEdge = ((edges & EDGE_BOTTOM) == EDGE_BOTTOM);
        final int itemCount = parent.getAdapter().getItemCount();
        if (itemCount > parent.getChildCount()) {
            final int width = c.getWidth();
            final int height = c.getHeight();

            if (wantsTopEdge && layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                gradientDrawable.setBounds(0, 0, width, edgeHeight);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                gradientDrawable.draw(c);
            }

            final int lastItem = itemCount - 1;
            if (wantsBottomEdge && layoutManager.findLastCompletelyVisibleItemPosition() < lastItem) {
                gradientDrawable.setBounds(0, height - edgeHeight, width, height);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                gradientDrawable.draw(c);
            }
        }
    }
}
