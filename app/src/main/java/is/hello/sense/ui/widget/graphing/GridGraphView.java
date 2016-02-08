package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.MultiAnimator;
import is.hello.sense.R;
import is.hello.sense.util.Logger;

public class GridGraphView extends LinearLayout
        implements GridRecycler.Adapter<LinearLayout, GridGraphCellView> {
    //region Constants

    private static final boolean DEBUG = true;

    private static final float TENSION = 0.9f;

    private static final float SCALE_MIN = 0.9f;
    private static final float SCALE_MAX = 1.0f;
    private static final float ALPHA_MIN = 0.0f;
    private static final float ALPHA_MAX = 1.0f;

    private static final long ROW_STAGGER = 50L;

    //endregion

    //region Rendering

    private final LayoutParams rowLayoutParams;
    private final LayoutParams cellLayoutParams;

    //endregion

    //region Contents

    private final GridRecycler<LinearLayout, GridGraphCellView> recycler;

    private final List<LinearLayout> rowViews = new ArrayList<>();
    private final Runnable populateCallback = this::populate;

    private final List<Animator> pendingCellAnimators = new ArrayList<>();

    //endregion

    //region Attribute Backing

    private @Nullable LayoutTransition parentLayoutTransition;
    private Adapter adapter;
    private @NonNull GridGraphCellView.Size cellSize = GridGraphCellView.Size.REGULAR;
    private int interRowPadding;

    //endregion


    //region Lifecycle

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

        final LayoutTransition layoutTransition = new LayoutTransition();

        layoutTransition.setAnimator(LayoutTransition.APPEARING, createAppearingAnimator());
        layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, createDisappearingAnimator());

        final AnticipateOvershootInterpolator rowInterpolator =
                new AnticipateOvershootInterpolator(TENSION);
        layoutTransition.setInterpolator(LayoutTransition.APPEARING, rowInterpolator);
        layoutTransition.setInterpolator(LayoutTransition.DISAPPEARING, rowInterpolator);

        layoutTransition.setInterpolator(LayoutTransition.CHANGE_APPEARING,
                                         new OvershootInterpolator(TENSION));
        layoutTransition.setInterpolator(LayoutTransition.CHANGE_DISAPPEARING,
                                         new AnticipateInterpolator(TENSION));

        layoutTransition.setDuration(Anime.DURATION_SLOW);

        setLayoutTransition(layoutTransition);

        this.cellLayoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        this.rowLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.interRowPadding = resources.getDimensionPixelSize(R.dimen.gap_small);

        this.recycler = new GridRecycler<>(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        for (final LinearLayout rowView : rowViews) {
            rowView.setPivotX(rowView.getMeasuredWidth() / 2f);
            rowView.setPivotY(0f);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // When a GridGraphView is contained in an outer ViewGroup which is animated
        // independently of its own LayoutAnimator, both its parent and itself must
        // disable child clipping for the animated layout changes to render correctly.
        final boolean clipChildren = (parentLayoutTransition == null);
        final ViewGroup parent = (ViewGroup) getParent();

        setClipChildren(clipChildren);
        parent.setClipChildren(clipChildren);

        setClipToPadding(clipChildren);
        parent.setClipToPadding(clipChildren);
    }

    //endregion


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

    public void bindParentLayoutTransition(@NonNull LayoutTransition parentLayoutTransition) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(),
                         "bindParentLayoutTransition(" + parentLayoutTransition + ")");
        }

        final LayoutTransition myLayoutTransition = getLayoutTransition();
        myLayoutTransition.setAnimateParentHierarchy(false);

        this.parentLayoutTransition = parentLayoutTransition;

        parentLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        parentLayoutTransition.setInterpolator(LayoutTransition.CHANGE_APPEARING,
                                               myLayoutTransition.getInterpolator(LayoutTransition.CHANGE_APPEARING));
        parentLayoutTransition.setInterpolator(LayoutTransition.CHANGE_DISAPPEARING,
                                               myLayoutTransition.getInterpolator(LayoutTransition.CHANGE_DISAPPEARING));

        parentLayoutTransition.setDuration(parentLayoutTransition.getDuration(LayoutTransition.CHANGING));

        // See #onAttachedToWindow() for explanation on these lines
        setClipChildren(false);
        setClipToPadding(false);
        if (getParent() != null) {
            final ViewGroup parent = (ViewGroup) getParent();
            parent.setClipChildren(false);
            parent.setClipToPadding(false);
        }
    }

    //endregion


    //region Recycler Adapter

    @Override
    public LinearLayout onCreateRowView() {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "onCreateRowView()");
        }

        final LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(rowLayoutParams);
        return row;
    }

    @Override
    public GridGraphCellView onCreateCellView() {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "onCreateCellView()");
        }

        final GridGraphCellView cell = new GridGraphCellView(getContext());
        cell.setLayoutParams(cellLayoutParams);
        return cell;
    }

    @Override
    public void onRowRecycled(@NonNull LinearLayout rowView) {
        removeView(rowView);
        rowViews.remove(rowView);
    }

    @Override
    public void onCellRecycled(@NonNull GridGraphCellView cellView) {
        // Do nothing.
    }

    //endregion


    //region Populating

    private void displayDataPoint(GridGraphCellView cellView,
                                  boolean includeAnimation,
                                  int row,
                                  int cell) {
        cellView.setSize(cellSize);
        cellView.setValue(adapter.getCellReading(row, cell));
        cellView.setBorder(adapter.getCellBorder(row, cell));

        final @ColorInt int cellColor = adapter.getCellColor(row, cell);
        if (includeAnimation) {
            final ValueAnimator fillColorAnimator = cellView.createFillColorAnimator(cellColor);
            if (fillColorAnimator != null) {
                pendingCellAnimators.add(fillColorAnimator);
            }
        } else {
            cellView.setFillColor(cellColor);
        }
    }

    private void runCellAnimators(long startDelay) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "runCellAnimators()");
        }

        if (!pendingCellAnimators.isEmpty()) {
            final AnimatorSet cellAnimator = new AnimatorSet();
            cellAnimator.setStartDelay(startDelay);
            cellAnimator.playTogether(pendingCellAnimators);
            cellAnimator.setDuration(Anime.DURATION_SLOW);
            cellAnimator.start();

            pendingCellAnimators.clear();
        }
    }

    private long shrinkRowCount(int delta) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "shrinkRowCount(" + delta + ")");
        }

        final long startDelay = (ROW_STAGGER * delta) / 2L;
        getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, startDelay);
        if (parentLayoutTransition != null) {
            parentLayoutTransition.setInterpolator(LayoutTransition.CHANGING,
                                                   getLayoutTransition().getInterpolator(LayoutTransition.CHANGE_DISAPPEARING));
            parentLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
            parentLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, startDelay);
        }

        for (int i = 0; i < delta; i++) {
            final LinearLayout rowView = rowViews.get(0);
            setAnimationIndex(rowView, delta - i);
            recycler.recycleRow(rowView);
        }

        return startDelay;
    }

    private long growRowCount(int delta) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "growRowCount(" + delta + ")");
        }

        final long startDelay = (ROW_STAGGER * delta) / 4L;
        getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_APPEARING, startDelay);
        if (parentLayoutTransition != null) {
            parentLayoutTransition.setInterpolator(LayoutTransition.CHANGING,
                                                   getLayoutTransition().getInterpolator(LayoutTransition.CHANGE_APPEARING));
            parentLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
            parentLayoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, startDelay);
        }

        for (int i = 0; i < delta; i++) {
            final LinearLayout rowView = recycler.dequeueRowView();
            setAnimationIndex(rowView, delta - i);
            rowViews.add(0, rowView);
            addView(rowView, 0);
        }

        return startDelay;
    }

    private void populateRows(int rowCount, long cellStartDelay, boolean includeCellAnimation) {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "populateRows(" + rowCount + ")");
        }

        final int lastRow = rowCount - 1;
        for (int row = 0; row < rowCount; row++) {
            final LinearLayout rowView = rowViews.get(row);
            final int cellCount = adapter.getRowCellCount(row);
            for (int cell = 0; cell < cellCount; cell++) {
                GridGraphCellView cellView = (GridGraphCellView) rowView.getChildAt(cell);
                boolean recycledCell = true;
                if (cellView == null) {
                    cellView = recycler.dequeueCellView();
                    rowView.addView(cellView);
                    recycledCell = false;
                }

                displayDataPoint(cellView, includeCellAnimation && recycledCell, row, cell);
            }

            recycler.recycleUnusedRowCells(rowView, cellCount);

            if (row != lastRow) {
                rowView.setPadding(0, 0, 0, interRowPadding);
            } else {
                rowView.setPadding(0, 0, 0, 0);
            }
        }

        runCellAnimators(cellStartDelay);
    }

    @VisibleForTesting
    void populate() {
        if (DEBUG) {
            Logger.debug(getClass().getSimpleName(), "populate()");
        }

        final int oldCount = rowViews.size();
        final int newCount = adapter != null ? adapter.getRowCount() : 0;
        final long cellStartDelay;
        if (newCount < oldCount) {
            cellStartDelay = shrinkRowCount(oldCount - newCount);
        } else if (newCount > oldCount) {
            cellStartDelay = growRowCount(newCount - oldCount);
        } else {
            cellStartDelay = 0L;
        }

        final boolean includeCellAnimation = (oldCount != 0);
        populateRows(newCount, cellStartDelay, includeCellAnimation);
    }

    @VisibleForTesting
    void requestPopulate() {
        removeCallbacks(populateCallback);
        post(populateCallback);
    }

    //endregion


    //region Attributes

    public void prime(int rows, int cellsPerRow) {
        recycler.prime(rows - rowViews.size(), cellsPerRow);
    }

    public void setAdapter(@Nullable Adapter adapter) {
        if (adapter != this.adapter) {
            if (this.adapter != null) {
                this.adapter.unregisterObserver(ADAPTER_OBSERVER);
            }

            this.adapter = adapter;

            if (adapter != null) {
                adapter.registerObserver(ADAPTER_OBSERVER);
            }

            requestPopulate();
        }
    }

    public void setCellSize(@NonNull GridGraphCellView.Size cellSize) {
        this.cellSize = cellSize;

        if (!rowViews.isEmpty()) {
            requestPopulate();
        }
    }

    public void setInterRowPadding(int interRowPadding) {
        this.interRowPadding = interRowPadding;

        if (!rowViews.isEmpty()) {
            requestPopulate();
        }
    }

    //endregion


    private final DataSetObserver ADAPTER_OBSERVER = new DataSetObserver() {
        @Override
        public void onChanged() {
            requestPopulate();
        }

        @Override
        public void onInvalidated() {
            setAdapter(null);
        }
    };

    public abstract static class Adapter {
        private final DataSetObservable observable = new DataSetObservable();

        public void registerObserver(@NonNull DataSetObserver observer) {
            observable.registerObserver(observer);
        }

        public void unregisterObserver(@NonNull DataSetObserver observer) {
            observable.unregisterObserver(observer);
        }

        public void notifyDataSetChanged() {
            observable.notifyChanged();
        }

        public abstract int getRowCount();
        public abstract int getRowCellCount(int row);
        public abstract @Nullable String getCellReading(int row, int cell);
        public abstract @ColorInt int getCellColor(int row, int cell);
        public abstract @NonNull GridGraphCellView.Border getCellBorder(int row, int cell);
    }
}
