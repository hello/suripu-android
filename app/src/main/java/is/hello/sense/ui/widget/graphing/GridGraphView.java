package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
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
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.adapter.TrendWeekAdapter;
import is.hello.sense.ui.widget.TrendCardView;
import is.hello.sense.ui.widget.util.Drawing;

public class GridGraphView extends LinearLayout
        implements GridRecycler.Adapter<LinearLayout, GridGraphCellView>, TrendCardView.OnBindGraph {
    //region Constants

    private static final boolean DEBUG = false;

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

    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final int estimatedTextHeight;

    //endregion

    //region Contents

    private final GridRecycler<LinearLayout, GridGraphCellView> recycler;

    private final ArrayList<LinearLayout> rowViews = new ArrayList<>();
    private final Runnable populateCallback = this::populate;

    private final ArrayList<Animator> pendingCellAnimators = new ArrayList<>();

    //endregion

    //region Attribute Backing

    private @Nullable LayoutTransition rootLayoutTransition;
    private Adapter adapter;
    private @NonNull GridGraphCellView.Size cellSize = GridGraphCellView.Size.REGULAR;
    private int interRowPadding;
    private @Nullable Integer highlightedTitle;
    private @Nullable List<String> titles;

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

        Drawing.updateTextPaintFromStyle(titlePaint, context,
                                         R.style.AppTheme_Text_Trends_BarGraph);
        this.estimatedTextHeight = Drawing.getEstimatedLineHeight(titlePaint, false);

        setOrientation(VERTICAL);

        final LayoutTransition layoutTransition = new AttachedLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

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
        final boolean clipChildren = (rootLayoutTransition == null);
        final ViewGroup parent = (ViewGroup) getParent();

        setClipChildren(clipChildren);
        parent.setClipChildren(clipChildren);

        setClipToPadding(clipChildren);
        parent.setClipToPadding(clipChildren);
    }

    //endregion


    //region Drawing Titles

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!Lists.isEmpty(titles)) {
            final int titlesCount = titles.size();
            if (titlesCount == 1) {
                canvas.drawText(titles.get(0),
                                0,
                                estimatedTextHeight,
                                titlePaint);
            } else {
                final int labelWidth = canvas.getWidth() / titlesCount;
                for (int i = 0; i < titlesCount; i++) {
                    canvas.drawText(titles.get(i),
                                    (labelWidth * i) + (labelWidth / 2f),
                                    estimatedTextHeight,
                                    titlePaint);
                }
            }
        }
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

    public void bindRootLayoutTransition(@NonNull LayoutTransition rootLayoutTransition) {
        if (DEBUG) {
            Log.d(getClass().getSimpleName(),
                  "bindRootLayoutTransition(" + rootLayoutTransition + ")");
        }

        final LayoutTransition myLayoutTransition = getLayoutTransition();
        myLayoutTransition.setAnimateParentHierarchy(false);

        this.rootLayoutTransition = rootLayoutTransition;

        rootLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        rootLayoutTransition.setInterpolator(LayoutTransition.CHANGE_APPEARING,
                                             myLayoutTransition.getInterpolator(LayoutTransition.CHANGE_APPEARING));
        rootLayoutTransition.setInterpolator(LayoutTransition.CHANGE_DISAPPEARING,
                                             myLayoutTransition.getInterpolator(LayoutTransition.CHANGE_DISAPPEARING));

        rootLayoutTransition.setDuration(rootLayoutTransition.getDuration(LayoutTransition.CHANGING));

        // See #onAttachedToWindow() for explanation on these lines
        setClipChildren(false);
        setClipToPadding(false);
        if (getParent() != null) {
            final ViewGroup parent = (ViewGroup) getParent();
            parent.setClipChildren(false);
            parent.setClipToPadding(false);
        }
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        final LayoutTransition myLayoutTransition = getLayoutTransition();
        if (animationsEnabled) {
            myLayoutTransition.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
            myLayoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            myLayoutTransition.enableTransitionType(LayoutTransition.APPEARING);
            myLayoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING);
            myLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        } else {
            myLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
            myLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            myLayoutTransition.disableTransitionType(LayoutTransition.APPEARING);
            myLayoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
            myLayoutTransition.disableTransitionType(LayoutTransition.CHANGING);
        }
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        if (!(getAdapter() instanceof TrendWeekAdapter)) {
            setAdapter(new TrendWeekAdapter(getContext()));
        }

        final TrendWeekAdapter adapter = (TrendWeekAdapter) getAdapter();
        adapter.bind(graph);

        // Weekly / monthly graphs always place the titles in the first section
        if (!graph.getSections().isEmpty()) {
            final GraphSection firstSection = graph.getSections().get(0);
            setTitles(firstSection.getHighlightedTitle(), firstSection.getTitles());
        }
    }

    //endregion


    //region Recycler Adapter

    @Override
    public LinearLayout onCreateRowView() {
        if (DEBUG) {
            Log.d(getClass().getSimpleName(), "onCreateRowView()");
        }

        final LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(rowLayoutParams);
        return row;
    }

    @Override
    public GridGraphCellView onCreateCellView() {
        if (DEBUG) {
            Log.d(getClass().getSimpleName(), "onCreateCellView()");
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
            Log.d(getClass().getSimpleName(), "runCellAnimators()");
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
            Log.d(getClass().getSimpleName(), "shrinkRowCount(" + delta + ")");
        }

        final long startDelay = (ROW_STAGGER * delta) / 2L;
        getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, startDelay);
        if (rootLayoutTransition != null) {
            rootLayoutTransition.setInterpolator(LayoutTransition.CHANGING,
                                                 getLayoutTransition().getInterpolator(LayoutTransition.CHANGE_DISAPPEARING));
            rootLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
            rootLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, startDelay);
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
            Log.d(getClass().getSimpleName(), "growRowCount(" + delta + ")");
        }

        final long startDelay = (ROW_STAGGER * delta) / 4L;
        getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_APPEARING, startDelay);
        if (rootLayoutTransition != null) {
            rootLayoutTransition.setInterpolator(LayoutTransition.CHANGING,
                                                 getLayoutTransition().getInterpolator(LayoutTransition.CHANGE_APPEARING));
            rootLayoutTransition.setStartDelay(LayoutTransition.CHANGING, startDelay);
            rootLayoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, startDelay);
        }

        for (int i = 0; i < delta; i++) {
            final LinearLayout rowView = recycler.dequeueRowView();
            rowView.setWeightSum(adapter.getMaximumRowCellCount());
            setAnimationIndex(rowView, delta - i);
            rowViews.add(0, rowView);
            addView(rowView, 0);
        }

        return startDelay;
    }

    private void populateRows(int rowCount, long cellStartDelay, boolean includeCellAnimation) {
        if (DEBUG) {
            Log.d(getClass().getSimpleName(), "populateRows(" + rowCount + ")");
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
            Log.d(getClass().getSimpleName(), "populate()");
        }

        removeCallbacks(populateCallback);

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

    /**
     * Pre-allocates all of the row and cell views necessary to render
     * the content matching the parameters passed into this method.
     *
     * @param rows  The number of rows that will be rendered.
     * @param cellsPerRow   The number of cells that will be rendered per row.
     */
    public void prime(int rows, int cellsPerRow) {
        recycler.prime(rows - rowViews.size(), cellsPerRow);
        rowViews.ensureCapacity(rows);
        pendingCellAnimators.ensureCapacity(cellsPerRow);
    }

    /**
     * Clears any scrap row and cell views in the {@code GridGraphView}'s {@link GridRecycler}.
     * <p>
     * Intended to be called from the trim memory callback in a fragment or activity.
     * See {@code ComponentCallbacks2#TRIM_MEMORY_RUNNING_LOW}.
     */
    public void emptyScrap() {
        recycler.emptyScrap();
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

            populate();
        }
    }

    public Adapter getAdapter() {
        return adapter;
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

    /**
     * Sets the titles to display above the data in the grid view.
     */
    public void setTitles(@Nullable Integer highlightedTitle, @Nullable List<String> titles) {
        if (DEBUG) {
            Log.d(getClass().getSimpleName(), "setTitles(" + highlightedTitle + ", " + titles + ")");
        }

        this.highlightedTitle = highlightedTitle;
        this.titles = titles;

        final boolean titlesEmpty = Lists.isEmpty(titles);
        if (titlesEmpty) {
            setPadding(0, 0, 0, 0);
        } else {
            final int estimatedLineHeight = Drawing.getEstimatedLineHeight(titlePaint, false);
            final int extra = getResources().getDimensionPixelSize(R.dimen.gap_small);
            setPadding(0, estimatedLineHeight + extra, 0, 0);

            if (titles.size() == 1) {
                titlePaint.setTextAlign(Paint.Align.LEFT);
            } else {
                titlePaint.setTextAlign(Paint.Align.CENTER);
            }
        }
        setWillNotDraw(titlesEmpty);
    }

    /**
     * Checks if the {@code GridGraphView} has any rows with content visible.
     * @return  false if there are no rows with content; true otherwise.
     */
    public boolean isEmpty() {
        return rowViews.isEmpty();
    }

    /**
     * Calculates the estimated vertical space a given number of rows requires to be displayed
     * in the {@code GridGraphView} with its current configuration.
     * <p>
     * Transitioning the height of both the {@code GridGraphView} and its parent can be a very
     * expensive animation. Using this method, combined with {@code #setMinimumHeight(int)}
     * allows code using {@code GridGraphView} to opportunistically size it before its populated
     * with content, potentially preventing the height transition entirely.
     *
     * @param rowCount  The estimated number of rows.
     * @return  The estimated height for the {@code GridGraphView}.
     *
     * @see #setCellSize(GridGraphCellView.Size)
     * @see #setInterRowPadding(int)
     */
    public int getEstimatedHeight(int rowCount) {
        final int rowHeight = cellSize.getHeight(getResources());
        return (rowHeight * rowCount) + (interRowPadding * (rowCount - 1));
    }

    //endregion


    private final DataSetObserver ADAPTER_OBSERVER = new DataSetObserver() {
        @Override
        public void onChanged() {
            populate();
        }

        @Override
        public void onInvalidated() {
            setAdapter(null);
        }
    };

    private class AttachedLayoutTransition extends LayoutTransition {
        @Override
        public boolean isTransitionTypeEnabled(int transitionType) {
            return isAttachedToWindow() && super.isTransitionTypeEnabled(transitionType);
        }
    }

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
        public abstract int getMaximumRowCellCount();
        public abstract @Nullable String getCellReading(int row, int cell);
        public abstract @ColorInt int getCellColor(int row, int cell);
        public abstract @NonNull GridGraphCellView.Border getCellBorder(int row, int cell);
    }
}
