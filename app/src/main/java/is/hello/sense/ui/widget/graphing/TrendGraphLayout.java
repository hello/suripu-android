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
public abstract class TrendGraphLayout extends LinearLayout implements TrendFeedViewItem.OnBindGraph {
    protected final TrendGraphView trendGraphView;


    private TrendGraphLayout(@NonNull Context context, @NonNull TrendGraphView trendGraphView) {
        super(context);
        this.trendGraphView = trendGraphView;
        setOrientation(VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        private final LinearLayout.LayoutParams horizontalRowLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        private final LinearLayout.LayoutParams quarterGridLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

        public GridTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new GridTrendGraphView(context, graph, animatorContext));
            final int verticalMargins = context.getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_quarter_vertical_margins);
            final int horizontalMargins = context.getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_quarter_horizontal_margins);
            horizontalRowLayoutParams.setMargins(0, verticalMargins, 0, verticalMargins);
            quarterGridLayoutParams.setMargins(horizontalMargins, 0, horizontalMargins, 0);
            currentTimescale = graph.getTimeScale();
            if (currentTimescale == Trends.TimeScale.LAST_3_MONTHS) {
                displayAsQuarterView(graph);
            } else {
                addView(trendGraphView);
            }
        }

        @Override
        public void bindGraph(@NonNull Graph graph) {

            if (this.currentTimescale == graph.getTimeScale()) {
                if (currentTimescale == Trends.TimeScale.LAST_3_MONTHS) {
                    ArrayList<Graph> graphs = graph.convertToQuarterGraphs();
                    if (graphViews.size() == graphs.size()) {
                        for (int i = 0; i < graphs.size(); i++) {
                            graphViews.get(i).bindGraph(graphs.get(i));
                        }
                        requestLayout();
                    }

                } else {
                    trendGraphView.bindGraph(graph);
                }
                return;
            }


            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                trendGraphView.fadeOut(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        displayAsQuarterView(graph);
                    }
                });
            } else if (currentTimescale == Trends.TimeScale.LAST_3_MONTHS) {
                for (int i = graphViews.size() - 1; i >= 0; i--) {
                    final GridTrendGraphView graphView = graphViews.get(i);
                    if (i == graphViews.size() - 1) {
                        graphView.fadeOut(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                trendGraphView.setAlpha(0);
                                addView(trendGraphView);
                                post(() -> {
                                    for (int i = getChildCount() - 2; i >= 0; i--) {
                                        removeViewAt(i);
                                    }
                                });
                                trendGraphView.bindGraph(graph);
                                trendGraphView.fadeIn(null);
                                requestLayout();
                            }
                        });
                    } else {
                        graphView.fadeOut(null);
                    }
                }
            } else {
                trendGraphView.bindGraph(graph);
            }
            this.currentTimescale = graph.getTimeScale();
        }

        private void displayAsQuarterView(@NonNull Graph graph) {
            graphViews.clear();
            ArrayList<Graph> graphs = graph.convertToQuarterGraphs();
            for (int i = 0; i < graphs.size(); i++) {
                final LinearLayout horizontalLayout;
                if (i % 2 == 0) {
                    horizontalLayout = addRow();
                } else {
                    horizontalLayout = getLastRow();
                }
                final GridTrendGraphView graphView = generateQuarterGridGraphView(graphs.get(i));
                horizontalLayout.addView(graphView);
                graphView.fadeIn(null);
                if (i == graphs.size() - 1) {
                    if (i % 2 == 0) {
                        LinearLayout placeHolder = new LinearLayout(getContext());
                        placeHolder.setLayoutParams(new LayoutParams(0, 1, 1));
                        horizontalLayout.addView(placeHolder);
                    }
                    graphView.post(() -> {
                        int viewToClear = indexOfChild(trendGraphView);
                        if (viewToClear != -1) {
                            graphView.fadeIn(null);
                            removeViewAt(viewToClear);
                        }
                    });
                }
                graphViews.add(graphView);

            }
        }

        private GridTrendGraphView generateQuarterGridGraphView(@NonNull Graph graph) {
            final GridTrendGraphView graphView = new GridTrendGraphView(getContext(), graph, getTrendGraphView().animatorContext);
            graphView.setLayoutParams(quarterGridLayoutParams);
            graphView.showText(false);
            graphView.setAlpha(0);
            return graphView;
        }

        private LinearLayout addRow() {
            final LinearLayout horizontalLayout = new LinearLayout(getContext());
            horizontalLayout.setLayoutParams(horizontalRowLayoutParams);
            horizontalLayout.setOrientation(HORIZONTAL);
            addView(horizontalLayout);
            return horizontalLayout;
        }

        private LinearLayout getLastRow() {
            if (getChildCount() > 0 && getChildAt(getChildCount() - 1) instanceof LinearLayout) {
                return (LinearLayout) getChildAt(getChildCount() - 1);
            }
            return addRow();
        }
    }

    public static class BarTrendGraphLayout extends TrendGraphLayout {

        public BarTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new BarTrendGraphView(context, graph, animatorContext));
            addView(trendGraphView);

        }

    }

    public static class BubbleTrendGraphLayout extends TrendGraphLayout {

        public BubbleTrendGraphLayout(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, new BubbleTrendGraphView(context, graph, animatorContext));
            addView(trendGraphView);
        }

    }
}