package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.EnumSet;

import is.hello.sense.R;
import is.hello.sense.ui.common.ScrollEdge;

public class FadingEdgesItemDecoration extends PaddingItemDecoration {
    private final LinearLayoutManager layoutManager;
    private final Drawable topEdge, bottomEdge;
    private final EnumSet<ScrollEdge> edges;

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources,
                                     @NonNull EnumSet<ScrollEdge> edges,
                                     @NonNull Style style) {
        super(new Rect());

        this.layoutManager = layoutManager;
        this.edges = edges;

        if (style == Style.ROUNDED_EDGES) {
            this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.rounded_shadow_top_down, null);
            this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.rounded_shadow_bottom_up, null);
        } else {
            this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_top_down, null);
            this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_bottom_up, null);
        }
    }

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources,
                                     @NonNull Style style) {
        this(layoutManager,
             resources,
             EnumSet.of(ScrollEdge.TOP, ScrollEdge.BOTTOM),
             style);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if(parent == null || parent.getAdapter() == null){
            return;
        }
        final int itemCount = parent.getAdapter().getItemCount();
        if (itemCount > 0 && layoutManager.canScrollVertically()) {
            final boolean wantsTopEdge = edges.contains(ScrollEdge.TOP);
            final boolean wantsBottomEdge = edges.contains(ScrollEdge.BOTTOM);
            final int width = c.getWidth();

            if (wantsTopEdge && layoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                topEdge.setBounds(insets.left, insets.top,
                                  width - insets.right,
                                  insets.top + topEdge.getIntrinsicHeight());
                topEdge.draw(c);
            }

            final int lastItem = itemCount - 1;
            if (wantsBottomEdge && layoutManager.findLastCompletelyVisibleItemPosition() < lastItem) {
                final int height = c.getHeight();
                bottomEdge.setBounds(insets.left,
                                     height - bottomEdge.getIntrinsicHeight() - insets.bottom,
                                     width - insets.right,
                                     height - insets.bottom);
                bottomEdge.draw(c);
            }
        }
    }

    public enum Style {
        STRAIGHT,
        ROUNDED_EDGES,
    }
}
