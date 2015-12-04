package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.EnumSet;

import is.hello.sense.R;
import is.hello.sense.ui.common.ScrollEdge;

public class FadingEdgesItemDecoration extends RecyclerView.ItemDecoration {
    private final LinearLayoutManager layoutManager;
    private final Drawable topEdge, bottomEdge;
    private final EnumSet<ScrollEdge> edges;

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources,
                                     @NonNull EnumSet<ScrollEdge> edges) {
        this.layoutManager = layoutManager;
        this.edges = edges;

        this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_top_down, null);
        this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_bottom_up, null);
    }

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources) {
        this(layoutManager, resources, EnumSet.of(ScrollEdge.TOP, ScrollEdge.BOTTOM));
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int itemCount = parent.getAdapter().getItemCount();
        if (itemCount > 0 && layoutManager.canScrollVertically()) {
            final boolean wantsTopEdge = edges.contains(ScrollEdge.TOP);
            final boolean wantsBottomEdge = edges.contains(ScrollEdge.BOTTOM);
            final int width = c.getWidth();

            if (wantsTopEdge && layoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                topEdge.setBounds(0, 0, width, topEdge.getIntrinsicHeight());
                topEdge.draw(c);
            }

            final int lastItem = itemCount - 1;
            if (wantsBottomEdge && layoutManager.findLastCompletelyVisibleItemPosition() < lastItem) {
                final int height = c.getHeight();
                bottomEdge.setBounds(0, height - bottomEdge.getIntrinsicHeight(), width, height);
                bottomEdge.draw(c);
            }
        }
    }
}
