package is.hello.sense.ui.widget.graphing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.sense.api.model.v2.Graph;

/**
 * +----------------------------+
 * | TrendGraphLayout           |
 * | +------------------------+ |
 * | | TrendGraphView         | |
 * | | +--------------------+ | |
 * | | | Controller         | | |
 * | | |                    | | |
 * | | +--------------------+ | |
 * | |                        | |
 * | | +--------------------+ | |
 * | | | TrendGraphDrawable | | |
 * | | |                    | | |
 * | | +--------------------+ | |
 * | +------------------------+ |
 * +----------------------------+
 */

/**
 * Layout for Graphs. Was mainly needed for the {@link GridTrendGraphView} Quarter view. A normal view doesn't have the
 * methods to quickly add and remove child views like a {@link LinearLayout}. This is essentially a view controller.
 */
@SuppressLint("ViewConstructor")
public class TrendGraphLayout extends LinearLayout implements TrendFeedViewItem.OnBindGraph {
    protected final TrendGraphView trendGraphView;


    public TrendGraphLayout(@NonNull Context context, @NonNull TrendGraphView trendGraphView) {
        super(context);
        this.trendGraphView = trendGraphView;
        setOrientation(VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(trendGraphView);
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        trendGraphView.bindGraph(graph);
    }

    @Override
    public boolean isAnimating() {
        return trendGraphView.isAnimating();
    }

    public TrendGraphView getTrendGraphView() {
        return trendGraphView;
    }

}