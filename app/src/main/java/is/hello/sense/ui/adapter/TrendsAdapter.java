package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;

public class TrendsAdapter extends ArrayRecyclerAdapter<TrendsPresenter.Rendered, TrendsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Resources resources;
    private final int graphTintColor;
    private final int VIEW_ERROR = 0;
    private final int VIEW_NO_TRENDS = 1;
    private final int VIEW_TRENDS = 2;
    private int viewType;
    private OnRetry onRetry;

    private @Nullable OnTrendOptionSelected onTrendOptionSelected;

    public TrendsAdapter(@NonNull Context context) {
        super(new ArrayList<>());

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.graphTintColor = resources.getColor(R.color.light_accent);
        this.viewType = VIEW_TRENDS;
    }


    public void setOnTrendOptionSelected(@Nullable OnTrendOptionSelected onTrendOptionSelected) {
        this.onTrendOptionSelected = onTrendOptionSelected;
    }

    public void displayNoDataMessage(OnRetry networkError){
        if (networkError == null){
            viewType = VIEW_NO_TRENDS;
            this.onRetry = null;
        } else {
            viewType = VIEW_ERROR;
            this.onRetry = networkError;
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean replaceAll(@NonNull Collection<? extends TrendsPresenter.Rendered> collection) {
        if (collection.size() > 0 ){
            viewType = VIEW_TRENDS;
        }
        return super.replaceAll(collection);
    }

    @Override
    public int getItemCount() {
        if (viewType != VIEW_TRENDS){
            return 1;
        }else{
            return super.getItemCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_ERROR: {
                final View view = inflater.inflate(R.layout.item_message_card, parent, false);
                view.findViewById(R.id.item_message_card_image).setVisibility(View.GONE);

                final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                title.setVisibility(View.GONE);

                final Button  action = (Button) view.findViewById(R.id.item_message_card_action);
                action.setText(R.string.action_retry);
                action.setOnClickListener(v -> {
                    if (onRetry != null) {
                        onRetry.fetchTrends();
                    }
                });


                final TextView message = (TextView) view.findViewById(R.id.item_message_card_message);
                message.setText(R.string.error_trends_unavailable);

                return new TrendsAdapter.ViewHolder(view);
            }
            case VIEW_NO_TRENDS:{
                final View view = inflater.inflate(R.layout.item_message_card, parent, false);
                ((ImageView)view.findViewById(R.id.item_message_card_image)).setImageResource(R.drawable.illustration_no_trends);
                view.findViewById(R.id.item_message_card_title).setVisibility(View.GONE);
                view.findViewById(R.id.item_message_card_action).setVisibility(View.GONE);
                ((TextView)view.findViewById(R.id.item_message_card_message)).setText(R.string.message_no_trends_yet);
                return new TrendsAdapter.ViewHolder(view);
            }
            case VIEW_TRENDS: {
                final View view = inflater.inflate(R.layout.item_trend, parent, false);
                return new ViewHolder(view);
            }
            default:{
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (viewType != VIEW_TRENDS){
            return;
        }
        final TrendsPresenter.Rendered rendered = getItem(position);

        final TrendGraph graph = rendered.graph;
        holder.title.setText(graph.getTitle());

        final GraphDrawable graphDrawable = graph.getGraphType().createDrawable(resources);
        holder.graphView.setGraphDrawable(graphDrawable);
        holder.graphView.setTintColor(graphTintColor);
        holder.graphView.setAdapter(holder.graphAdapter);
        holder.graphView.setHeaderFooterProvider(holder.graphAdapter);

        if (graph.getOptions() == null || graph.getOptions().size() < 2) {
            holder.removeOptionSelector();
        } else {
            holder.addOptionSelector(position, graph.getOptions(), graph.getTimePeriod());
        }

        holder.graphView.setNumberOfLines(TrendGraphAdapter.getNumberOfLines(graph));
        holder.graphView.setGridDrawable(graph.getGraphType().getGridDrawable());
        holder.graphAdapter.bind(rendered);
        if (graphDrawable instanceof LineGraphDrawable) {
            final LineGraphDrawable lineGraph = (LineGraphDrawable) graphDrawable;
            lineGraph.setMarkers(holder.graphAdapter.getMarkers());
        }
    }


    class ViewHolder extends ArrayRecyclerAdapter.ViewHolder
            implements SelectorView.OnSelectionChangedListener {
        final ViewGroup itemView;
        final TextView title;
        final GraphView graphView;
        final TrendGraphAdapter graphAdapter;
        @Nullable SelectorView optionSelector;

        ViewHolder(@NonNull View view) {
            super(view);

            this.itemView = (ViewGroup) view;
            this.title = (TextView) view.findViewById(R.id.item_trend_title);
            this.graphView = (GraphView) view.findViewById(R.id.item_trend_graph);
            this.graphAdapter = new TrendGraphAdapter(resources);
        }


        void addOptionSelector(int index, @NonNull List<String> options, @NonNull String selectedOption) {
            if (optionSelector == null) {
                this.optionSelector = new SelectorView(itemView.getContext());
                optionSelector.setBackground(new TabsBackgroundDrawable(resources,
                                                                        TabsBackgroundDrawable.Style.INLINE));
                optionSelector.setOnSelectionChangedListener(this);
                itemView.addView(optionSelector, 0);
            } else {
                optionSelector.removeAllButtons();
                optionSelector.setEnabled(true);
            }

            for (final String option : options) {
                final ToggleButton button = optionSelector.addOption(option, option, true);
                optionSelector.setButtonTag(button, option);
                if (option.equals(selectedOption)) {
                    optionSelector.setSelectedButton(button);
                }
            }

            optionSelector.setTag(index);
            optionSelector.synchronize();
        }

        void removeOptionSelector() {
            if (optionSelector != null) {
                itemView.removeView(optionSelector);
                this.optionSelector = null;

            }
        }

        @Override
        public void onSelectionChanged(int newSelectionIndex) {
            if (optionSelector != null && onTrendOptionSelected != null) {
                final int index = (int) optionSelector.getTag();
                final String option = optionSelector.getButtonTagAt(newSelectionIndex).toString();
                onTrendOptionSelected.onTrendOptionSelected(index, option);

                optionSelector.setEnabled(false);
            }
        }
    }

    public interface OnRetry{
        void fetchTrends();
    }

    public interface OnTrendOptionSelected {
        void onTrendOptionSelected(int trendIndex, @NonNull String option);
    }
}
