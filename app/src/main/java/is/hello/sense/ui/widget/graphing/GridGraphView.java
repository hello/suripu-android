package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import java.util.ArrayDeque;
import java.util.Deque;

import is.hello.sense.R;

public class GridGraphView extends LinearLayout {
    private final LayoutParams rowLayoutParams;
    private final LayoutParams cellLayoutParams;
    private final int interRowPadding;

    private final Deque<LinearLayout> rowScrap = new ArrayDeque<>();
    private final Deque<GridDataPointView> cellScrap = new ArrayDeque<>();

    private Adapter adapter;

    public GridGraphView(@NonNull Context context) {
        this(context, null);
    }

    public GridGraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridGraphView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();

        setOrientation(VERTICAL);
        setShowDividers(SHOW_DIVIDER_MIDDLE);

        this.rowLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.cellLayoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        this.interRowPadding = resources.getDimensionPixelSize(R.dimen.gap_small);
    }

    //region Vending Views

    private LinearLayout createRow(@NonNull Context context) {
        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(rowLayoutParams);
        return row;
    }

    private GridDataPointView createCell(@NonNull Context context) {
        final GridDataPointView cell = new GridDataPointView(context);
        cell.setLayoutParams(cellLayoutParams);
        return cell;
    }

    private LinearLayout dequeueRow(@NonNull Context context) {
        final LinearLayout scrap = rowScrap.poll();
        if (scrap != null) {
            return scrap;
        } else {
            return createRow(context);
        }
    }

    private GridDataPointView dequeueCell(@NonNull Context context) {
        final GridDataPointView scrap = cellScrap.poll();
        if (scrap != null) {
            return scrap;
        } else {
            return createCell(context);
        }
    }

    private void recycleRow(@NonNull LinearLayout row) {
        for (int i = 0, count = row.getChildCount(); i < count; i++) {
            cellScrap.offer((GridDataPointView) row.getChildAt(i));
        }
        removeView(row);
        rowScrap.offer(row);
    }

    //endregion

    //region Populating

    private void displayDataPoint(GridDataPointView itemView, int row, int item) {
        itemView.setValue(adapter.getValueReading(row, item));
        itemView.setFillColor(adapter.getValueColor(row, item));
    }

    private void populate(int oldRowCount, int newRowCount) {
        if (newRowCount < oldRowCount) {
            final int toRemove = oldRowCount - newRowCount;
            for (int i = 0; i < toRemove; i++) {
                recycleRow((LinearLayout) getChildAt(0));
            }
        }

        final Context context = getContext();
        final int lastRow = newRowCount - 1;
        for (int row = 0; row < newRowCount; row++) {
            final LinearLayout rowView = dequeueRow(context);
            final int itemCount = adapter.getSectionItemCount(row);
            for (int item = 0; item < itemCount; item++) {
                final GridDataPointView itemView = dequeueCell(context);
                displayDataPoint(itemView, row, item);
                rowView.addView(itemView, cellLayoutParams);
            }

            if (row != lastRow) {
                rowView.setPadding(0, 0, 0, interRowPadding);
            } else {
                rowView.setPadding(0, 0, 0, 0);
            }

            addView(rowView, rowLayoutParams);
        }
    }

    //endregion

    public void setAdapter(@Nullable Adapter adapter) {
        final int oldRowCount = this.adapter != null ? this.adapter.getSectionCount() : 0;
        final int newRowCount = adapter != null ? adapter.getSectionCount() : 0;

        this.adapter = adapter;
        populate(oldRowCount, newRowCount);
    }

    public interface Adapter {
        int getSectionCount();
        int getSectionItemCount(int section);
        String getValueReading(int section, int item);
        @ColorInt int getValueColor(int section, int item);
        boolean isValueHighlighted(int section, int item);
    }
}
