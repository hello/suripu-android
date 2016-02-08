package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.ui.widget.graphing.GridGraphCellView;
import is.hello.sense.ui.widget.graphing.GridGraphView;

public class TrendMonthAdapter extends GridGraphView.Adapter {
    public static final int ROW_LIMIT = 7;

    private final Context context;
    private Graph graph;
    private @NonNull List<List<Float>> sections = Collections.emptyList();

    //region Lifecycle

    public TrendMonthAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void bind(@NonNull Graph graph, @NonNull GraphSection section) {
        this.graph = graph;
        this.sections = Lists.partition(section.getValues(), ROW_LIMIT);
        notifyDataSetChanged();
    }

    public void clear() {
        this.graph = null;
        this.sections = Collections.emptyList();
        notifyDataSetChanged();
    }

    //endregion


    //region Providing Data

    @Override
    public int getRowCount() {
        return sections.size();
    }

    @Override
    public int getRowCellCount(int row) {
        return sections.get(row).size();
    }

    @Nullable
    @Override
    public String getCellReading(int row, int cell) {
        return null;
    }

    @Override
    @ColorInt
    public int getCellColor(int row, int cell) {
        final Float value = sections.get(row).get(cell);
        if (value != null) {
            if (value < 0f) {
                return ContextCompat.getColor(context, R.color.graph_grid_empty_missing);
            } else {
                final Condition condition = graph.getConditionForValue(value);
                return ContextCompat.getColor(context, condition.colorRes);
            }
        } else {
            return ContextCompat.getColor(context, R.color.graph_grid_empty_cell);
        }
    }

    @NonNull
    @Override
    public GridGraphCellView.Border getCellBorder(int row, int cell) {
        final Float value = sections.get(row).get(cell);
        if (value == null) {
            return GridGraphCellView.Border.OUTSIDE;
        } else {
            return GridGraphCellView.Border.NONE;
        }
    }

    //endregion
}
