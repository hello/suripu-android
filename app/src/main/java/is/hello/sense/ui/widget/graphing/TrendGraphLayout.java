package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.BarTrendGraphView;
import is.hello.sense.ui.widget.BubbleTrendGraphView;
import is.hello.sense.ui.widget.GridTrendGraphView;

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
 * Layout for Graphs. Mainly needed for the {@link GridTrendGraphView} Quarter view. A normal view doesn't have the
 * methods to quickly add and remove child views like a {@link LinearLayout}. This is essentially a view controller.
 */
@SuppressLint("ViewConstructor")
public class TrendGraphLayout extends LinearLayout implements TrendFeedViewItem.OnBindGraph {
    protected final TrendGraphView trendGraphView;


    private TrendGraphLayout(@NonNull Context context, @NonNull TrendGraphView trendGraphView) {
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

    public TrendGraphView getTrendGraphView() {
        return trendGraphView;
    }

    /**
     * Manages how {@link GridTrendGraphView} gets laid out. Changes it's layout based on the
     * {@link is.hello.sense.api.model.v2.Trends.TimeScale} of the {@link Graph}.
     */
    public static class GridTrendGraphLayout extends TrendGraphLayout {
        private Trends.TimeScale currentTimescale;
        private ArrayList<GridTrendGraphView> graphViews = new ArrayList<>();
        private final int verticalMargins;
        private final int horizontalMargins;

        public GridTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new GridTrendGraphView(context, graph, animatorContext));
            this.verticalMargins = context.getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_quarter_vertical_margins);
            this.horizontalMargins = context.getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_quarter_horizontal_margins);
        }

        @Override
        public void bindGraph(@NonNull Graph graph) {

            if (this.currentTimescale == graph.getTimeScale()) {
                return;
            }


            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                Animator.AnimatorListener animatorListener = new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        removeAllViews();
                        ArrayList<Graph> graphs = graph.convertToQuarterGraphs();
                        for (int i = 0; i < graphs.size(); i++) {
                            final LinearLayout horizontalLayout;
                            if (i % 2 == 0) {
                                horizontalLayout = new LinearLayout(getContext());
                                LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                horizontalLayoutParams.setMargins(0, verticalMargins, 0, verticalMargins);
                                horizontalLayout.setLayoutParams(horizontalLayoutParams);
                                horizontalLayout.setOrientation(HORIZONTAL);
                                addView(horizontalLayout);
                            } else {
                                horizontalLayout = (LinearLayout) getChildAt(getChildCount() - 1);
                            }
                            GridTrendGraphView graphView = new GridTrendGraphView(getContext(), graphs.get(i), getTrendGraphView().animatorContext);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                            layoutParams.setMargins(horizontalMargins, 0, horizontalMargins, 0);

                            graphView.setLayoutParams(layoutParams);
                            graphView.showText(false);
                            graphView.setAlpha(0);
                            horizontalLayout.addView(graphView);
                            graphView.fadeIn(null);
                            graphViews.add(graphView);
                        }
                    }
                };
                trendGraphView.fadeOut(animatorListener);

            } else if (currentTimescale == Trends.TimeScale.LAST_3_MONTHS) {
                for (int i = 0; i < graphViews.size(); i++) {
                    GridTrendGraphView graphView = graphViews.get(i);
                    if (i == graphViews.size() - 1) {
                        Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                removeAllViews();
                                addView(trendGraphView);
                                trendGraphView.bindGraph(graph);
                                trendGraphView.fadeIn(null);
                            }
                        };
                        graphView.fadeOut(listener);
                    } else {
                        graphView.fadeOut(null);
                    }
                }
            } else {
                trendGraphView.bindGraph(graph);
            }
            this.currentTimescale = graph.getTimeScale();
        }
    }

    public static class BarTrendGraphLayout extends TrendGraphLayout {

        public BarTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new BarTrendGraphView(context, graph, animatorContext));
        }

    }

    public static class BubbleTrendGraphLayout extends TrendGraphLayout {

        public BubbleTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new BubbleTrendGraphView(context, graph, animatorContext));
        }

    }
}