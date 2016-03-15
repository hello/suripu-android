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
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.BarTrendGraphView;
import is.hello.sense.ui.widget.BubbleTrendGraphView;
import is.hello.sense.ui.widget.GridTrendGraphView;

public class TrendFeedView extends LinearLayout {
    private static final int DAYS_IN_WEEK = 7;

    private Trends trends;
    private AnimatorContext animatorContext;
    private boolean loading = false;

    private final Map<Graph.GraphType, TrendFeedViewItem> cardViews = new HashMap<>(3);
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

    public void setLoading(boolean loading) {
        if (loading != this.loading) {
            this.loading = loading;

            for (final TrendFeedViewItem cardView : cardViews.values()) {
                cardView.setLoading(loading);
            }
        }
    }

    public void bindTrends(@NonNull Trends trends) {
        setLoading(false);

        if (errorCard != null) {
            removeView(errorCard);
            this.errorCard = null;
        }

        this.trends = trends;
        populate();
    }

    public void presentError(@NonNull TrendFeedViewItem.OnRetry onRetry) {
        setLoading(false);

        this.trends = null;

        removeAllViews();
        cardViews.clear();

        if (errorCard != null) {
            removeView(errorCard);
        }

        this.errorCard = TrendFeedViewItem.createErrorCard(getContext(), onRetry);
        addView(errorCard);

    }

    private void populate() {
        final List<Graph> graphs = trends.getGraphs();
        if (graphs.isEmpty()) {
            if (welcomeCard == null) {
                if (trends.getAvailableTimeScales().isEmpty()) {
                    this.welcomeCard = TrendFeedViewItem.createWelcomeCard(getContext());
                } else {
                    this.welcomeCard = TrendFeedViewItem.createWelcomeBackCard(getContext());
                }
                setLayoutTransition(null);
                addView(welcomeCard);
                setLayoutTransition(new LayoutTransition());
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
                    this.welcomeCard = TrendFeedViewItem.createComingSoonCard(getContext(), days);
                    addView(welcomeCard);
                }
            }
        } else if (welcomeCard != null) {
            removeView(welcomeCard);
            welcomeCard = null;
        }

        final Set<Graph.GraphType> includedTypes = new HashSet<>(graphs.size());
        for (final Graph graph : graphs) {
            final Graph.GraphType graphType = graph.getSpecConformingGraphType();
            TrendFeedViewItem trendFeedViewItem = cardViews.get(graphType);
            if (trendFeedViewItem == null) {
                // todo erase one day start>
                if (graphType == Graph.GraphType.OVERVIEW) {
                    TrendFeedViewItem temp = cardViews.get(Graph.GraphType.GRID);
                    if (temp != null) {
                        setLayoutTransition(null);
                        temp.setVisibility(INVISIBLE);
                        removeView(temp);
                        setLayoutTransition(new LayoutTransition());
                    }
                } else if (graphType == Graph.GraphType.GRID) {
                    TrendFeedViewItem temp = cardViews.get(Graph.GraphType.OVERVIEW);
                    if (temp != null) {
                        setLayoutTransition(null);
                        temp.setVisibility(INVISIBLE);
                        removeView(temp);
                        setLayoutTransition(new LayoutTransition());
                    }
                }
                // todo erase one day end>
                trendFeedViewItem = createTrendCard(graph);
                setLayoutTransition(null); // todo erase one day.
                trendFeedViewItem.setVisibility(INVISIBLE);
                cardViews.put(graphType, trendFeedViewItem);
                addView(trendFeedViewItem, getPositionAffinity(graph));
                trendFeedViewItem.setVisibility(VISIBLE);
                setLayoutTransition(new LayoutTransition()); // todo erase one day.

            } else {
                trendFeedViewItem.bindGraph(graph);
            }
            trendFeedViewItem.setLoading(loading);

            includedTypes.add(graphType);
        }

        final Iterator<Map.Entry<Graph.GraphType, TrendFeedViewItem>> cardViewsIterator =
                cardViews.entrySet().iterator();
        while (cardViewsIterator.hasNext()) {
            final Map.Entry<Graph.GraphType, TrendFeedViewItem> entry = cardViewsIterator.next();
            if (!includedTypes.contains(entry.getKey())) {
                removeView(entry.getValue());
                cardViewsIterator.remove();
            }
        }
        if (getChildCount() > 0) {
            ((LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams()).bottomMargin = getResources().getDimensionPixelSize(R.dimen.gap_outer_half);
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

    private TrendFeedViewItem createTrendCard(@NonNull Graph graph) {
        final Context context = getContext();
        switch (graph.getSpecConformingGraphType()) {
            case BAR:
                return new TrendFeedViewItem(new BarTrendGraphView(context, graph, animatorContext));

            case BUBBLES:
                return new TrendFeedViewItem(new BubbleTrendGraphView(context, graph, animatorContext));

            case GRID:
                return new TrendFeedViewItem(new GridTrendGraphView(context, graph, animatorContext));

            case OVERVIEW:
                final MultiGridGraphView multiGridGraphView = new MultiGridGraphView(context);
                return TrendFeedViewItem.createMultiGridCard(multiGridGraphView, graph);

            default:
                throw new IllegalArgumentException("Unknown graph type " + graph.getSpecConformingGraphType());
        }
    }
}
