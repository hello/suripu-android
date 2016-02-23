package is.hello.sense.ui.widget.graphing;

import android.animation.LayoutTransition;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.TrendCardView;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;

public class TrendFeedView extends LinearLayout {
    private static final int DAYS_IN_WEEK = 7;

    private Trends trends;
    private AnimatorContext animatorContext;

    private final Map<Graph.GraphType, TrendCardView> cardViews = new HashMap<>(3);
    private
    @Nullable
    View welcomeCard;
    private
    @Nullable
    View errorCard;


    public TrendFeedView(@NonNull Context context) {
        this(context, null);
    }

    public TrendFeedView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrendFeedView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutTransition(new LayoutTransition());
    }


    public void setAnimatorContext(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void bindTrends(@NonNull Trends trends) {
        if (errorCard != null) {
            removeView(errorCard);
            this.errorCard = null;
        }

        this.trends = trends;
        populate();
    }

    public void presentError(@NonNull TrendCardView.OnRetry onRetry) {
        this.trends = null;

        removeAllViews();
        cardViews.clear();

        if (errorCard != null) {
            removeView(errorCard);
        }

        this.errorCard = TrendCardView.createErrorCard(getContext(), onRetry);
        addView(errorCard);

    }

    private void populate() {
        final List<Graph> graphs = trends.getGraphs();
        if (graphs.isEmpty()) {
            if (welcomeCard == null) {
                if (trends.getAvailableTimeScales().isEmpty()) {
                    this.welcomeCard = TrendCardView.createWelcomeBackCard(getContext());
                } else {
                    this.welcomeCard = TrendCardView.createWelcomeCard(getContext());
                }
                addView(welcomeCard);
            }
        } else if (graphs.size() == 1) {
            final Graph graph = graphs.get(0);
            if (graph.isGrid() && welcomeCard == null) {
                int numberOfDaysWithValues = 0;
                outer:
                for (final GraphSection section : graph.getSections()) {
                    for (final Float value : section.getValues()) {
                        if (value != null) {
                            numberOfDaysWithValues++;
                            if (numberOfDaysWithValues == DAYS_IN_WEEK) {
                                break outer;
                            }
                        }
                    }
                }

                final int days = DAYS_IN_WEEK - numberOfDaysWithValues;
                if (days > 0) {
                    this.welcomeCard = TrendCardView.createComingSoonCard(getContext(), days);
                    addView(welcomeCard);
                }
            }
        } else if (welcomeCard != null) {
            removeView(welcomeCard);
        }

        final Set<Graph.GraphType> includedTypes = new HashSet<>(graphs.size());
        for (final Graph graph : graphs) {
            final Graph.GraphType graphType = graph.getSpecConformingGraphType();
            TrendCardView trendCardView = cardViews.get(graphType);
            if (trendCardView == null) {
                trendCardView = createTrendCard(graph);
                cardViews.put(graphType, trendCardView);
                addView(trendCardView, getPositionAffinity(graph));
            } else {
                trendCardView.bindGraph(graph);
            }

            includedTypes.add(graphType);
        }

        final Iterator<Map.Entry<Graph.GraphType, TrendCardView>> cardViewsIterator =
                cardViews.entrySet().iterator();
        while (cardViewsIterator.hasNext()) {
            final Map.Entry<Graph.GraphType, TrendCardView> entry = cardViewsIterator.next();
            if (!includedTypes.contains(entry.getKey())) {
                removeView(entry.getValue());
                cardViewsIterator.remove();
            }
        }
    }

    private static int getPositionAffinity(@NonNull Graph graph) {
        switch (graph.getSpecConformingGraphType()) {
            case GRID:
            case OVERVIEW:
                return 0;

            default:
                return -1; // end
        }
    }

    private TrendCardView createTrendCard(@NonNull Graph graph) {
        final Context context = getContext();
        switch (graph.getSpecConformingGraphType()) {
            case BAR:
                final BarGraphDrawable barGraphDrawable = new BarGraphDrawable(context, graph, animatorContext);
                final TrendGraphView barGraphView = new TrendGraphView(context, barGraphDrawable);
                return TrendCardView.createGraphCard(barGraphView, graph);

            case BUBBLES:
                final BubbleGraphDrawable bubbleGraphDrawable = new BubbleGraphDrawable(context, graph, animatorContext);
                final TrendGraphView bubbleGraphView = new TrendGraphView(context, bubbleGraphDrawable);
                return TrendCardView.createGraphCard(bubbleGraphView, graph);

            case GRID:
                final GridGraphView gridGraphView = new GridGraphView(context);
                gridGraphView.bindRootLayoutTransition(getLayoutTransition());
                return TrendCardView.createGridCard(gridGraphView, graph);

            case OVERVIEW:
                final MultiGridGraphView multiGridGraphView = new MultiGridGraphView(context);
                return TrendCardView.createMultiGridCard(multiGridGraphView, graph);

            default:
                throw new IllegalArgumentException("Unknown graph type " + graph.getSpecConformingGraphType());
        }
    }
}
