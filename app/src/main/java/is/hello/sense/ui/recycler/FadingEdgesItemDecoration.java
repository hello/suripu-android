package is.hello.sense.ui.recycler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
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
    private final Drawable topEdge, bottomEdge;
    private final @Edges int edges;

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources,
                                     @Edges int edges) {
        this.layoutManager = layoutManager;
        this.edges = edges;

        this.topEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_bottom, null);
        this.bottomEdge = ResourcesCompat.getDrawable(resources, R.drawable.shadow_top, null);
    }

    public FadingEdgesItemDecoration(@NonNull LinearLayoutManager layoutManager,
                                     @NonNull Resources resources) {
        this(layoutManager, resources, EDGE_TOP | EDGE_BOTTOM);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int itemCount = parent.getAdapter().getItemCount();
        if (itemCount > 0 && layoutManager.canScrollVertically()) {
            final boolean wantsTopEdge = ((edges & EDGE_TOP) == EDGE_TOP);
            final boolean wantsBottomEdge = ((edges & EDGE_BOTTOM) == EDGE_BOTTOM);
            final int width = c.getWidth();

            if (wantsTopEdge && layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
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
