package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.MultiAnimator;
import is.hello.sense.R;

public class GridGraphView extends LinearLayout {
    private static final int MSG_POPULATE = 0x606;

    private static final float TENSION = 0.5f;

    private static final float SCALE_MIN = 0.9f;
    private static final float SCALE_MAX = 1.0f;
    private static final float ALPHA_MIN = 0.0f;
    private static final float ALPHA_MAX = 1.0f;

    private static final long ROW_STAGGER = 50L;

    private final LayoutParams rowLayoutParams;
    private final LayoutParams cellLayoutParams;
    private final int interRowPadding;

    private final Deque<LinearLayout> rowScrap = new ArrayDeque<>();
    private final Deque<GridGraphCellView> cellScrap = new ArrayDeque<>();
    private final List<LinearLayout> rows = new ArrayList<>();

    private final Runnable populateCallback = this::populate;
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

        final LayoutTransition layoutTransition = new LayoutTransition();

        layoutTransition.setAnimator(LayoutTransition.APPEARING, createAppearingAnimator());
        layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, createDisappearingAnimator());

        final AnticipateInterpolator anticipate = new AnticipateInterpolator(TENSION);
        final AnticipateOvershootInterpolator anticipateOvershoot =
                new AnticipateOvershootInterpolator(TENSION);
        layoutTransition.setInterpolator(LayoutTransition.APPEARING, anticipate);
        layoutTransition.setInterpolator(LayoutTransition.DISAPPEARING, anticipateOvershoot);

        layoutTransition.setInterpolator(LayoutTransition.CHANGE_APPEARING, anticipateOvershoot);
        layoutTransition.setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, anticipate);

        layoutTransition.setDuration(Anime.DURATION_SLOW);

        setLayoutTransition(layoutTransition);

        this.cellLayoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        this.rowLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.interRowPadding = resources.getDimensionPixelSize(R.dimen.gap_small);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        for (final LinearLayout row : rows) {
            row.setPivotX(row.getMeasuredWidth() / 2f);
            row.setPivotY(0f);
        }
    }


    //region Animations

    private static void setAnimationIndex(@NonNull View rowView, int index) {
        rowView.setTag(R.id.view_grid_graph_animation_index, index);
    }

    private static int getAnimationIndex(@NonNull View rowView) {
        final Integer index = (Integer) rowView.getTag(R.id.view_grid_graph_animation_index);
        return index != null ? index : -1;
    }

    private static void clearAnimationIndex(@NonNull View rowView) {
        rowView.setTag(R.id.view_grid_graph_animation_index, null);
    }

    private static Animator createAppearingAnimator() {
        return MultiAnimator.empty()
                            .addOnAnimationWillStart(animator -> {
                                final View rowView = animator.getTarget();
                                rowView.setScaleX(SCALE_MIN);
                                rowView.setScaleY(SCALE_MIN);
                                rowView.setAlpha(ALPHA_MIN);

                                final Integer index = getAnimationIndex(rowView);
                                if (index != -1) {
                                    animator.setStartDelay(index * ROW_STAGGER);
                                    clearAnimationIndex(rowView);
                                }
                            })
                            .alpha(ALPHA_MAX)
                            .scale(SCALE_MAX);
    }

    private static Animator createDisappearingAnimator() {
        return MultiAnimator.empty()
                            .addOnAnimationWillStart(animator -> {
                                final View rowView = animator.getTarget();
                                final Integer index = getAnimationIndex(rowView);
                                if (index != -1) {
                                    animator.setStartDelay(index * ROW_STAGGER);
                                    clearAnimationIndex(rowView);
                                }
                            })
                            .alpha(ALPHA_MIN)
                            .scale(SCALE_MIN);
    }

    //endregion


    //region Vending Views

    private LinearLayout createRowView(@NonNull Context context) {
        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(rowLayoutParams);
        return row;
    }

    private GridGraphCellView createCellView(@NonNull Context context) {
        final GridGraphCellView cell = new GridGraphCellView(context);
        cell.setLayoutParams(cellLayoutParams);
        return cell;
    }

    private LinearLayout dequeueRowView(@NonNull Context context) {
        LinearLayout row = rowScrap.poll();
        if (row == null) {
            row = createRowView(context);
        }
        return row;
    }

    private GridGraphCellView dequeueCellView(@NonNull Context context) {
        GridGraphCellView cell = cellScrap.poll();
        if (cell == null) {
            cell = createCellView(context);
        }
        return cell;
    }

    private void recycleUnusedRowCells(@NonNull LinearLayout rowView,
                                       int targetCount) {
        while (rowView.getChildCount() > targetCount) {
            final int lastChildIndex = rowView.getChildCount() - 1;
            final GridGraphCellView cellView = (GridGraphCellView) rowView.getChildAt(lastChildIndex);
            cellScrap.offer(cellView);
            rowView.removeViewAt(lastChildIndex);
        }
    }

    private void recycleRow(@NonNull LinearLayout row) {
        removeView(row);
        rows.remove(row);
        rowScrap.offer(row);
    }

    //endregion

    //region Populating

    private void displayDataPoint(GridGraphCellView itemView, int row, int item) {
        itemView.setValue(adapter.getCellReading(row, item));
        itemView.setFillColor(adapter.getCellColor(row, item));
        itemView.setBorder(adapter.getCellBorder(row, item));
    }

    private void populate() {
        final Context context = getContext();
        final int oldRowCount = rows.size();
        final int newRowCount = adapter != null ? adapter.getRowCount() : 0;
        if (newRowCount < oldRowCount) {
            final int toRemove = (oldRowCount - newRowCount);

            getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_DISAPPEARING,
                                                (ROW_STAGGER * toRemove) / 2L);

            for (int i = 0; i < toRemove; i++) {
                final LinearLayout rowView = rows.get(0);
                setAnimationIndex(rowView, toRemove - i);
                recycleRow(rowView);
            }
        } else if (newRowCount > oldRowCount) {
            final int toAdd = (newRowCount - oldRowCount);

            getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_APPEARING,
                                                (ROW_STAGGER * toAdd) / 4L);

            for (int i = 0; i < toAdd; i++) {
                final LinearLayout rowView = dequeueRowView(context);
                setAnimationIndex(rowView, toAdd - i);
                rows.add(0, rowView);
                addView(rowView, 0);
            }
        }

        final int lastRow = newRowCount - 1;
        for (int row = 0; row < newRowCount; row++) {
            final LinearLayout rowView = rows.get(row);
            final int cellCount = adapter.getRowCellCount(row);
            for (int cell = 0; cell < cellCount; cell++) {
                GridGraphCellView cellView = (GridGraphCellView) rowView.getChildAt(cell);
                if (cellView == null) {
                    cellView = dequeueCellView(context);
                    rowView.addView(cellView);
                }

                displayDataPoint(cellView, row, cell);
            }

            recycleUnusedRowCells(rowView, cellCount);

            if (row != lastRow) {
                rowView.setPadding(0, 0, 0, interRowPadding);
            } else {
                rowView.setPadding(0, 0, 0, 0);
            }
        }
    }

    private void postPopulate() {
        removeCallbacks(populateCallback);
        post(populateCallback);
    }

    //endregion

    public void setAdapter(@Nullable Adapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterObserver(ADAPTER_OBSERVER);
        }

        this.adapter = adapter;

        if (adapter != null) {
            adapter.registerObserver(ADAPTER_OBSERVER);
        }

        postPopulate();
    }

    private final DataSetObserver ADAPTER_OBSERVER = new DataSetObserver() {
        @Override
        public void onChanged() {
            postPopulate();
        }

        @Override
        public void onInvalidated() {
            setAdapter(null);
        }
    };

    public abstract static class Adapter {
        private final DataSetObservable observable = new DataSetObservable();

        public void registerObserver(DataSetObserver observer) {
            observable.registerObserver(observer);
        }

        public void unregisterObserver(DataSetObserver observer) {
            observable.unregisterObserver(observer);
        }

        public void unregisterAll() {
            observable.unregisterAll();
        }

        public void notifyDataSetChanged() {
            observable.notifyChanged();
        }

        public abstract int getRowCount();
        public abstract int getRowCellCount(int row);
        public abstract @Nullable String getCellReading(int row, int cell);
        public abstract @ColorInt int getCellColor(int row, int cell);
        public abstract @Nullable GridGraphCellView.Border getCellBorder(int row, int cell);
    }
}
