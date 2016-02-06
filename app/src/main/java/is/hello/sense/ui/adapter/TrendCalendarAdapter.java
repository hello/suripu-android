package is.hello.sense.ui.adapter;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.ui.widget.graphing.GridGraphCellView;
import is.hello.sense.ui.widget.graphing.GridGraphView;

public class TrendCalendarAdapter extends GridGraphView.Adapter {
    private final Resources resources;
    private Graph graph;

    //region Lifecycle

    public TrendCalendarAdapter(@NonNull Resources resources) {
        this.resources = resources;
    }

    public void bindTrendGraph(@NonNull Graph graph) {
        this.graph = graph;
        notifyDataSetChanged();
    }

    public void clear() {
        this.graph = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Providing Data

    @Override
    public int getRowCount() {
        if (graph != null) {
            return graph.getSections().size();
        } else {
            return 0;
        }
    }

    @Override
    public int getRowCellCount(int row) {
        return graph.getSections().get(row).getValues().size();
    }

    @Nullable
    @Override
    public String getCellReading(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value != null) {
            if (value < 0f) {
                return resources.getString(R.string.missing_data_placeholder);
            } else {
                return String.format("%.0f", value);
            }
        } else {
            return null;
        }
    }

    @Override
    public int getCellColor(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value != null) {
            if (value < 0f) {
                return resources.getColor(R.color.graph_grid_empty_missing);
            } else {
                final Condition condition = graph.getConditionForValue(value);
                return resources.getColor(condition.colorRes);
            }
        } else {
            return resources.getColor(R.color.graph_grid_empty_cell);
        }
    }

    @Nullable
    @Override
    public GridGraphCellView.Border getCellBorder(int row, int cell) {
        final GraphSection section = graph.getSections().get(row);
        final Float value = section.getValues().get(cell);
        if (value == null) {
            return GridGraphCellView.BORDER_OUTSIDE;
        } else if (section.getHighlightedValues().contains(cell)) {
            return GridGraphCellView.BORDER_INSIDE;
        } else {
            return null;
        }
    }

    //endregion
}
