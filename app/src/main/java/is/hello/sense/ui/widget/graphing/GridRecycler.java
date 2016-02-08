package is.hello.sense.ui.widget.graphing;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.util.Logger;

public class GridRecycler<Row extends ViewGroup, Cell extends View> {
    private static final boolean DEBUG = true;

    private final Adapter<Row, Cell> adapter;

    private final List<Row> rowScrap = new ArrayList<>();
    private final List<Cell> cellScrap = new ArrayList<>();

    private int maxRowScrapCount = 3;
    private int maxCellScrapCount = 7;

    GridRecycler(@NonNull Adapter<Row, Cell> adapter) {
        this.adapter = adapter;
    }

    public void setMaxRowScrapCount(int maxRowScrapCount) {
        this.maxRowScrapCount = maxRowScrapCount;

        while (rowScrap.size() > maxRowScrapCount) {
            rowScrap.remove(0);
        }
    }

    public void setMaxCellScrapCount(int maxCellScrapCount) {
        this.maxCellScrapCount = maxCellScrapCount;

        while (cellScrap.size() > maxCellScrapCount) {
            cellScrap.remove(0);
        }
    }

    public void prime(int rows, int cellsPerRow) {
        Log.d(getClass().getSimpleName(), "prime(" + rows + ", " + cellsPerRow + ")");

        for (int row = 0; row < rows; row++) {
            final Row rowView = adapter.onCreateRowView();
            for (int cell = 0; cell < cellsPerRow; cell++) {
                rowView.addView(adapter.onCreateCellView());
            }
            rowScrap.add(rowView);
        }
    }

    public Row dequeueRowView() {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "dequeueRowView()");
        }

        if (rowScrap.isEmpty()) {
            return adapter.onCreateRowView();
        } else {
            final Row rowView = rowScrap.get(0);
            rowScrap.remove(0);
            return rowView;
        }
    }

    public Cell dequeueCellView() {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "dequeueCellView()");
        }

        if (cellScrap.isEmpty()) {
            return adapter.onCreateCellView();
        } else {
            final Cell cellView = cellScrap.get(0);
            cellScrap.remove(0);
            return cellView;
        }
    }

    public void recycleUnusedRowCells(@NonNull Row rowView, int targetCount) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "recycleUnusedRowCells(" + rowView +
                    ", " + targetCount + ")");
        }

        while (rowView.getChildCount() > targetCount) {
            final int lastChildIndex = rowView.getChildCount() - 1;
            @SuppressWarnings("unchecked")
            final Cell cellView = (Cell) rowView.getChildAt(lastChildIndex);
            if (cellScrap.size() < maxCellScrapCount) {
                cellScrap.add(cellView);
            }
            rowView.removeViewAt(lastChildIndex);
            adapter.onCellRecycled(cellView);
        }
    }

    public void recycleRow(@NonNull Row rowView) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "recycleRow(" + rowView + ")");
        }

        if (rowScrap.size() < maxRowScrapCount) {
            rowScrap.add(rowView);
        }
        adapter.onRowRecycled(rowView);
    }

    public interface Adapter<Row extends ViewGroup, Cell extends View> {
        Row onCreateRowView();

        Cell onCreateCellView();

        void onRowRecycled(@NonNull Row rowView);

        void onCellRecycled(@NonNull Cell cellView);
    }
}
