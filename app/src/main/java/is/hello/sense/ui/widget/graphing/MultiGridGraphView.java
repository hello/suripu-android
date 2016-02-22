package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.ui.adapter.TrendMonthAdapter;
import is.hello.sense.ui.widget.TrendCardView;

public class MultiGridGraphView extends LinearLayout implements TrendCardView.OnBindGraph {
    private static final int ROW_COUNT = 2;
    private static final int COLUMN_COUNT = 2;

    private final List<GridGraphView> graphViews = new ArrayList<>(ROW_COUNT * COLUMN_COUNT);

    public MultiGridGraphView(@NonNull Context context) {
        this(context, null);
    }

    public MultiGridGraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiGridGraphView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(VERTICAL);

        final LayoutParams rowLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                              LayoutParams.WRAP_CONTENT);
        final LayoutParams columnLayoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        for (int row = 0; row < ROW_COUNT; row++) {
            final LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(HORIZONTAL);
            for (int column = 0; column < COLUMN_COUNT; column++) {
                final GridGraphView graphView = createGridGraphView(context);
                graphViews.add(graphView);
                rowLayout.addView(graphView, columnLayoutParams);
            }
            addView(rowLayout, rowLayoutParams);
        }
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        final List<GraphSection> sections = graph.getSections();
        final int sectionCount = sections.size();
        for (int i = 0, count = graphViews.size(); i < count; i++) {
            final GridGraphView graphView = graphViews.get(i);
            final TrendMonthAdapter adapter = (TrendMonthAdapter) graphView.getAdapter();
            if (i < sectionCount) {
                graphView.setVisibility(VISIBLE);
                adapter.bind(graph, sections.get(i));
            } else {
                graphView.setVisibility(GONE);
                adapter.clear();
            }
        }
    }

    private GridGraphView createGridGraphView(@NonNull Context context) {
        final GridGraphView view = new GridGraphView(context);
        view.setCellSize(GridGraphCellView.Size.SMALL);
        view.setAdapter(new TrendMonthAdapter(context));
        view.setAnimationsEnabled(false);
        view.setMinimumHeight(view.getEstimatedHeight(5));
        return view;
    }
}
