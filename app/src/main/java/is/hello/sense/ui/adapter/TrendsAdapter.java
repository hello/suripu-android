package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.GraphType;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;

public class TrendsAdapter extends ArrayAdapter<TrendGraph> {
    private final LayoutInflater inflater;
    private final Resources resources;
    private final int graphTintColor;

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

    @SuppressWarnings("UnusedParameters")
    public void trendsUnavailable(Throwable e) {
        clear();
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
        holder.graphView.setNumberOfLines(graph.getDataPoints().size());
        holder.graphView.setAdapter(holder.graphAdapter);
        holder.graphView.setWantsHeaders(graph.getGraphType() == GraphType.HISTOGRAM);
        holder.graphView.setHeaderFooterProvider(holder.graphAdapter);

        holder.graphAdapter.setTrendGraph(graph);

        return view;
    }


    class ViewHolder {
        final TextView title;
        final GraphView graphView;
        final TrendGraphAdapter graphAdapter;

        ViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_trend_title);
            this.graphView = (GraphView) view.findViewById(R.id.item_trend_graph);
            this.graphAdapter = new TrendGraphAdapter(resources);
        }
    }
}
