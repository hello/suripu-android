package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
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
    private final Deque<GridDataPointView> cellScrap = new ArrayDeque<>();
    private final List<LinearLayout> rows = new ArrayList<>();

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
        LinearLayout row = rowScrap.poll();
        if (row == null) {
            row = createRow(context);
        }
        return row;
    }

    private GridDataPointView dequeueCell(@NonNull Context context) {
        GridDataPointView cell = cellScrap.poll();
        if (cell == null) {
            cell = createCell(context);
        }
        return cell;
    }

    private void recycleUnusedRowCells(@NonNull LinearLayout row, int targetCount) {
        while (row.getChildCount() > targetCount) {
            final int lastView = row.getChildCount() - 1;
            final GridDataPointView cell = (GridDataPointView) row.getChildAt(lastView);
            cellScrap.offer(cell);
            row.removeViewAt(lastView);
        }
    }

    private void recycleRow(@NonNull LinearLayout row) {
        removeView(row);
        rows.remove(row);
        rowScrap.offer(row);
    }

    //endregion

    //region Populating

    private void displayDataPoint(GridDataPointView itemView, int row, int item) {
        itemView.setValue(adapter.getValueReading(row, item));
        itemView.setFillColor(adapter.getValueColor(row, item));
        itemView.setBorder(adapter.getValueBorder(row, item));
    }

    private void populate(int oldRowCount, int newRowCount) {
        final Context context = getContext();
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
                final LinearLayout rowView = dequeueRow(context);
                setAnimationIndex(rowView, toAdd - i);
                rows.add(0, rowView);
                addView(rowView, 0);
            }
        }

        final int lastRow = newRowCount - 1;
        for (int row = 0; row < newRowCount; row++) {
            final LinearLayout rowView = rows.get(row);
            final int itemCount = adapter.getSectionItemCount(row);
            for (int item = 0; item < itemCount; item++) {
                GridDataPointView itemView = (GridDataPointView) rowView.getChildAt(item);
                if (itemView == null) {
                    itemView = dequeueCell(context);
                    rowView.addView(itemView);
                }

                displayDataPoint(itemView, row, item);
            }

            recycleUnusedRowCells(rowView, itemCount);

            if (row != lastRow) {
                rowView.setPadding(0, 0, 0, interRowPadding);
            } else {
                rowView.setPadding(0, 0, 0, 0);
            }
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
        GridDataPointView.Border getValueBorder(int section, int item);
    }
}
