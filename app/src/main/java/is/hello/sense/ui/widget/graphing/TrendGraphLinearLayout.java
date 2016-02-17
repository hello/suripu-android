package is.hello.sense.ui.widget.graphing;

import android.animation.LayoutTransition;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

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
        this(context, null);
    }

    public TrendGraphLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrendGraphLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutTransition(new LayoutTransition());
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
            TrendLayout item =(TrendLayout)findViewWithTag(graphType);
            if (item != null) {
                item.bindGraph(graph);
                continue;
            }
            switch (graph.getGraphType()) {
                case BAR:
                    final BarGraphDrawable barGraphDrawable = new BarGraphDrawable(getContext(), graph, animatorContext);
                    final TrendCardView barGraphView = new TrendCardView(getContext(), barGraphDrawable);
                    addView(TrendLayout.getGraphItem(getContext(), graph, barGraphView));
                    break;
                case BUBBLES:
                    final BubbleGraphDrawable bubbleGraphDrawable = new BubbleGraphDrawable(getContext(), graph, animatorContext);
                    final TrendCardView bubbleGraphView = new TrendCardView(getContext(), bubbleGraphDrawable);
                    addView(TrendLayout.getGraphItem(getContext(), graph, bubbleGraphView));
                    break;
                case GRID:
                    final GridGraphView gridGraphView = new GridGraphView(getContext());
                    gridGraphView.bindParentLayoutTransition(getLayoutTransition());
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


}
