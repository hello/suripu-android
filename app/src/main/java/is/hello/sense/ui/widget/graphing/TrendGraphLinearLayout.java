package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.RoundedLinearLayout;
import is.hello.sense.ui.widget.TrendLayout;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;

/**
 * Temporary class for quickly displaying trends.
 */
public class TrendGraphLinearLayout extends RoundedLinearLayout {
    private Trends trends;
    private boolean isError;
    private AnimatorContext animatorContext;

    public TrendGraphLinearLayout(@NonNull Context context) {
        super(context);
    }

    public TrendGraphLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TrendGraphLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setAnimatorContext(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void update(Trends trends) {
        if (isError) {
            isError = false;
            removeAllViews();
        }
        this.trends = trends;
        inflateTrends();
    }

    public void presentError(TrendLayout.OnRetry onRetry) {
        isError = true;
        removeAllViews();
        addView(TrendLayout.getErrorItem(getContext(), onRetry));

    }

    private void inflateTrends() {
        for (Graph graph : trends.getGraphs()) {
            Graph.GraphType graphType = graph.getGraphType();
            TrendLayout item = getViewForGraph(graphType);
            if (item != null) {
                item.updateGraph(graph);
                continue;
            }
            switch (graph.getGraphType()) {
                case BAR:
                    final BarGraphDrawable barGraphDrawable = new BarGraphDrawable(getContext(), graph, animatorContext);
                    final TrendView barGraphView = new TrendView(getContext(), barGraphDrawable);
                    addView(TrendLayout.getGraphItem(getContext(), graph, barGraphView));
                    break;
                case BUBBLES:
                    final BubbleGraphDrawable bubbleGraphDrawable = new BubbleGraphDrawable(getContext(), graph, animatorContext);
                    final TrendView bubbleGraphView = new TrendView(getContext(), bubbleGraphDrawable);
                    addView(TrendLayout.getGraphItem(getContext(), graph, bubbleGraphView));
                    break;
                case GRID:
                    final GridGraphView gridGraphView = new GridGraphView(getContext());
                    addView(TrendLayout.getGridGraphItem(getContext(), graph, gridGraphView));
                    break;
                case OVERVIEW:
                    break;
            }
        }
        if (getChildCount() > 0) {
            ((LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams()).bottomMargin = 0;
        }
    }

    private TrendLayout getViewForGraph(Graph.GraphType graphType) {
        for (int i = 0; i < getChildCount(); i++) {
            View item = getChildAt(i);
            if (item.getTag() == graphType) {
                return (TrendLayout) item;
            }
        }
        return null;
    }


}
