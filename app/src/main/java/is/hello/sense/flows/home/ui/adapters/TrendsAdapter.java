package is.hello.sense.flows.home.ui.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.databinding.ItemTrendsBinding;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.graphing.trends.BarTrendGraphView;
import is.hello.sense.ui.widget.graphing.trends.BubbleTrendGraphView;
import is.hello.sense.ui.widget.graphing.trends.GridTrendGraphView;
import is.hello.sense.ui.widget.graphing.trends.TrendFeedViewItem;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphLayout;
import is.hello.sense.ui.widget.graphing.trends.TrendGraphView;

public class TrendsAdapter extends ArrayRecyclerAdapter<Graph, TrendsAdapter.BaseViewHolder> {
    private static final int WELCOME_TYPE = 0;
    private static final int TREND_TYPE = 1;
    private static final int ERROR_TYPE = 2;

    private final AnimatorContext animatorContext;
    private final TrendGraphView.AnimationCallback animationCallback;
    private final TrendFeedViewItem.OnRetry onRetry;
    private final boolean accountIsMoreThan2WeeksOld;

    private boolean showError = false;

    public TrendsAdapter(@NonNull final AnimatorContext animatorContext,
                         @NonNull final TrendGraphView.AnimationCallback animationCallback,
                         @NonNull final TrendFeedViewItem.OnRetry onRetry,
                         final boolean accountIsMoreThan2WeeksOld) {
        super(new ArrayList<>());
        this.animatorContext = animatorContext;
        this.animationCallback = animationCallback;
        this.accountIsMoreThan2WeeksOld = accountIsMoreThan2WeeksOld;
        this.onRetry = onRetry;
    }

    @Override
    public int getItemCount() {
        if (super.getItemCount() == 0) {
            return 1;
        }
        return super.getItemCount();
    }

    @Override
    public int getItemViewType(final int position) {
        if (getItemCount() == 1) {
            return showError ? ERROR_TYPE : WELCOME_TYPE;
        }
        return TREND_TYPE;
    }

    @Override
    public TrendsAdapter.BaseViewHolder onCreateViewHolder(final ViewGroup parent,
                                                           final int viewType) {
        switch (viewType) {
            case ERROR_TYPE:
                return new BaseViewHolder(TrendFeedViewItem.createErrorCard(parent.getContext(), onRetry));
            case WELCOME_TYPE:
                if (accountIsMoreThan2WeeksOld) {
                    return new BaseViewHolder(TrendFeedViewItem.createWelcomeBackCard(parent.getContext()));
                } else {
                    return new BaseViewHolder(TrendFeedViewItem.createWelcomeCard(parent.getContext()));
                }
            default:
                return new TrendViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trends, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final TrendsAdapter.BaseViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    public void setTrends(@NonNull final Trends trends) {
        if (!hasNewGraphs(trends)) {
            return;
        }
        this.showError = false;
        clear();
        if (trends.getGraphs() != null) {
            addAll(trends.getGraphs());
        }

        notifyDataSetChanged();
    }

    public void showError() {
        this.showError = true;
        clear();
        notifyDataSetChanged();
    }

    private boolean hasNewGraphs(@NonNull final Trends trends) {
        if (showError) {
            return true; // We are showing an error card. Update.
        }
        if (trends.getGraphs() == null) {
            return getItemCount() != 1; // Wants to show welcome card. Confirm that we're not already showing it.
        }
        if (getItemCount() == 1) {
            return true; // We are showing a welcome card. Update.
        }
        for (int i = 0; i < getItemCount(); i++) {
            if (!trends.getGraphs().contains(getItem(i))) {
                return true; // New graph. Update.
            }
        }
        return false; // All graphs were found.
    }

    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        public void bind(final int position) {

        }
    }


    private class TrendViewHolder extends BaseViewHolder {
        private final ItemTrendsBinding itemTrendsBinding;

        private TrendViewHolder(@NonNull final View itemView) {
            super(itemView);
            itemTrendsBinding = DataBindingUtil.bind(itemView);
        }

        @Override
        public void bind(final int position) {
            final Graph graph = getItem(position);
            final TrendGraphView trendGraphView;
            final Context context = itemTrendsBinding.itemTrendsContainer.getContext();
            switch (graph.getGraphType()) {
                case GRID:
                    trendGraphView = new GridTrendGraphView(context,
                                                            graph,
                                                            animatorContext,
                                                            animationCallback);
                    break;
                case BAR:
                    trendGraphView = new BarTrendGraphView(context,
                                                           graph,
                                                           animatorContext,
                                                           animationCallback);
                    break;
                case BUBBLES:
                    trendGraphView = new BubbleTrendGraphView(context,
                                                              graph,
                                                              animatorContext,
                                                              animationCallback);
                    break;
                default:
                    return;
            }
            itemTrendsBinding.itemTrendsContainer.getContext();
            itemTrendsBinding.itemTrendsContainer.addView(new TrendFeedViewItem(new TrendGraphLayout(context, trendGraphView)));
        }
    }
}
