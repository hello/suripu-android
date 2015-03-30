package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.GraphType;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;

public class TrendsAdapter extends ArrayAdapter<TrendGraph> {
    private final LayoutInflater inflater;
    private final Resources resources;
    private final int graphTintColor;

    private @Nullable OnTrendOptionSelected onTrendOptionSelected;

    public TrendsAdapter(@NonNull Context context) {
        super(context, R.layout.item_trend);

        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.graphTintColor = resources.getColor(R.color.light_accent);
    }


    public void bindTrends(@NonNull ArrayList<TrendGraph> trends) {
        clear();
        addAll(trends);
    }

    public void setOnTrendOptionSelected(@Nullable OnTrendOptionSelected onTrendOptionSelected) {
        this.onTrendOptionSelected = onTrendOptionSelected;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_trend, parent, false);
            view.setTag(new ViewHolder(view));
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        TrendGraph graph = getItem(position);

        holder.title.setText(graph.getTitle());

        GraphDrawable graphDrawable = graph.getGraphType().createDrawable(resources);
        holder.graphView.setGraphDrawable(graphDrawable);
        holder.graphView.setTintColor(graphTintColor);
        holder.graphView.setAdapter(holder.graphAdapter);
        holder.graphView.setWantsHeaders(graph.getGraphType() == GraphType.HISTOGRAM);
        holder.graphView.setHeaderFooterProvider(holder.graphAdapter);

        if (graph.getOptions() == null || graph.getOptions().size() < 2) {
            holder.removeOptionSelector();
        } else {
            holder.addOptionSelector(position, graph.getOptions(), graph.getTimePeriod());
        }

        holder.graphView.setNumberOfLines(TrendGraphAdapter.getNumberOfLines(graph));
        holder.graphAdapter.bindTrendGraph(graph, () -> {
            if (graphDrawable instanceof LineGraphDrawable) {
                ((LineGraphDrawable) graphDrawable).setMarkers(holder.graphAdapter.getMarkers());
            }
        });

        return view;
    }


    class ViewHolder implements SelectorLinearLayout.OnSelectionChangedListener {
        final ViewGroup itemView;
        final TextView title;
        final GraphView graphView;
        final TrendGraphAdapter graphAdapter;
        @Nullable SelectorLinearLayout optionSelector;

        ViewHolder(@NonNull View view) {
            this.itemView = (ViewGroup) view;
            this.title = (TextView) view.findViewById(R.id.item_trend_title);
            this.graphView = (GraphView) view.findViewById(R.id.item_trend_graph);
            this.graphAdapter = new TrendGraphAdapter(resources);
        }


        void addOptionSelector(int index, @NonNull List<String> options, @NonNull String selectedOption) {
            if (optionSelector == null) {
                this.optionSelector = new SelectorLinearLayout(getContext());
                optionSelector.setBackground(new TabsBackgroundDrawable(resources, options.size()));
                optionSelector.setOnSelectionChangedListener(this);
                itemView.addView(optionSelector, 0);
            } else {
                optionSelector.removeAllOptions();
            }

            for (String option : options) {
                int optionIndex = optionSelector.addOption(option, option);
                if (option.equals(selectedOption)) {
                    optionSelector.setSelectedIndex(optionIndex);
                }
            }

            optionSelector.setTag(index);
            optionSelector.synchronizeButtonStates();
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
                int index = (int) optionSelector.getTag();
                String option = optionSelector.getButtonTag(newSelectionIndex).toString();
                onTrendOptionSelected.onTrendOptionSelected(index, option);
            }
        }
    }


    public interface OnTrendOptionSelected {
        void onTrendOptionSelected(int trendIndex, @NonNull String option);
    }
}
