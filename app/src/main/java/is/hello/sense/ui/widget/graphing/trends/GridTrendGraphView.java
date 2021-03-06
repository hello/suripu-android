package is.hello.sense.ui.widget.graphing.trends;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

/**
 * View to show a graphs Grid type.
 */
@SuppressLint("ViewConstructor")
public class GridTrendGraphView extends TrendGraphView {
    private final static int DEFAULT_CELLS = 7;
    private final static int MANY_CELLS = 15;
    private final int graphWidth;
    private final AnimatorContext animatorContext;
    private boolean showText = true;

    public GridTrendGraphView(@NonNull final Context context,
                              @NonNull final Graph graph,
                              @NonNull final AnimatorContext animatorContext,
                              @NonNull final AnimationCallback animationCallback) {
        super(context, animatorContext, animationCallback);
        final int margin = context.getResources().getDimensionPixelSize(R.dimen.x1);
        // There are other views with margins that we need to subtract from screen width so the circles fit.
        // One view has x1 on each side another has x2 on each side and another has x.5 on each side.
        final int allMargins = margin * 6 + margin / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.graphWidth = screenWidth - allMargins;
        this.animatorContext = animatorContext;
        this.drawable = new GridGraphDrawable(context, graph, animatorContext);
        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawable.showGraphAnimation();
        ((GridGraphDrawable) drawable).initHeight();
        bindGraph(drawable.getGraph());
    }

    @Override
    public void bindGraph(@NonNull final Graph graph) {
        isAnimating = true;
        final Graph oldGraph = getGraph();
        if (getAlpha() == 0) { // No need to animate, the view isn't visible.
            setDrawableHeight(getDrawableHeight(graph));
            requestLayout();
            finishedAnimating();
            super.bindGraph(graph);
            return;
        }
        if (oldGraph.getTimeScale() == graph.getTimeScale()) { // just update the data
            super.bindGraph(graph);
            setDrawableHeight(getDrawableHeight(graph));
            requestLayout();
            finishedAnimating();
            return;
        }
        final int currentHeight = getDrawableHeight();
        if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS || oldGraph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
            final ValueAnimator shrinkAnimator = getHeightChangeAnimator(maxAnimationFactor, minAnimationFactor, currentHeight);
            shrinkAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    GridTrendGraphView.super.bindGraph(graph);
                    ((GridGraphDrawable) drawable).initHeight();
                    final int targetHeight = getDrawableHeight(graph);
                    final ValueAnimator expandAnimator = getHeightChangeAnimator(minAnimationFactor, maxAnimationFactor, targetHeight);
                    expandAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(final Animator animation) {
                            finishedAnimating();
                        }
                    });
                    animatorContext.startWhenIdle(expandAnimator);
                }
            });
            animatorContext.startWhenIdle(shrinkAnimator);

        } else {
            final int targetHeight = getDrawableHeight(graph);
            final ValueAnimator animator = ValueAnimator.ofFloat(minAnimationFactor, maxAnimationFactor);
            animator.setDuration(Anime.DURATION_NORMAL);
            animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
            animator.addUpdateListener(a -> {
                if (targetHeight > currentHeight) {
                    setDrawableHeight((int) (currentHeight + (targetHeight - currentHeight) * (float) a.getAnimatedValue()));
                } else {
                    setDrawableHeight((int) (targetHeight + (currentHeight - targetHeight) * (maxAnimationFactor - (float) a.getAnimatedValue())));
                }
                requestLayout();

            });
            if (targetHeight > currentHeight) {
                // The drawable is increasing in size, add cells immediately so they appear as it's height increases.
                super.bindGraph(graph);
            }
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    ((GridGraphDrawable) drawable).initHeight(graph);
                    if (!GridTrendGraphView.this.getGraph().equals(graph)) {
                        GridTrendGraphView.super.bindGraph(graph);
                    }
                    finishedAnimating();
                }
            });

            animatorContext.startWhenIdle(animator);
        }
    }

    private ValueAnimator getHeightChangeAnimator(final float start,
                                                  final float end,
                                                  final float height) {
        final ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(450);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            setDrawableHeight((int) (height * (float) a.getAnimatedValue()));
            requestLayout();
        });
        return animator;
    }

    private float getCircleSize() {
        return ((GridGraphDrawable) drawable).circleSize;
    }

    private int getDrawableHeight() {
        return getDrawableHeight(drawable.getGraph());
    }

    private int getDrawableHeight(@NonNull final Graph graph) {
        return ((GridGraphDrawable) drawable).getHeight(graph);
    }

    private void setDrawableHeight(final int height) {
        ((GridGraphDrawable) drawable).height = height;
    }

    /**
     * Show/Hide the value of each cell.
     *
     * @param show show text.
     */
    public void showText(final boolean show) {
        this.showText = show;
    }


    /**
     * Drawable that represents the Grid of a given Graph Object.
     */
    private class GridGraphDrawable extends TrendGraphDrawable {

        private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textCellPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whiteTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint emptyCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int textHeight;
        private final int inset = getContext().getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_inset);

        private GridCellController cellController;
        private int height = 0;
        private float circleSize = 0;
        private float padding = 0;
        private float reservedTopSpace = 0;
        private float radius = 0;

        private GridGraphDrawable(@NonNull final Context context,
                                  @NonNull final Graph graph,
                                  @NonNull final AnimatorContext animatorContext) {
            super(context, graph, animatorContext);
            Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.Caption1_Hint);
            Drawing.updateTextPaintFromStyle(textCellPaint, context, R.style.BodySmall);
            whiteTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
            cardPaint.setColor(ContextCompat.getColor(getContext(), R.color.background_card));
            emptyCellPaint.setColor(ContextCompat.getColor(context, R.color.trends_grid_graph_cell_empty));
            textCellPaint.setColor(whiteTextPaint.getColor());
            final Rect bounds = new Rect();
            textLabelPaint.getTextBounds("A", 0, 1, bounds);
            textHeight = bounds.height();
        }

        @Override
        public int getIntrinsicHeight() {
            return height + (int) reservedTopSpace;
        }

        @Override
        public void updateGraph(@NonNull final Graph graph) {
            this.graph = graph;
            updateCellController();
            invalidate();
        }

        @Override
        public void draw(@NonNull final Canvas canvas) {
            // Draw Text Labels
            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                final ArrayList<Graph> quarterGraphs = this.graph.getQuarterGraphs();
                for (int h = 0; h < quarterGraphs.size(); h++) {
                    final Graph graph = quarterGraphs.get(h);
                    for (final GraphSection section : graph.getSections()) {
                        final List<String> titles = section.getTitles();
                        for (int i = 0; i < titles.size(); i++) {
                            final String title = titles.get(i);
                            final float leftPos = i * circleSize;
                            final Rect textTitleBounds = new Rect();
                            textLabelPaint.getTextBounds(title, 0, title.length(), textTitleBounds);
                            float leftOffset = 0;
                            float topOffset = 0;
                            if (h % 2 != 0) {
                                leftOffset += (canvas.getWidth() + circleSize) / 2;
                            }
                            if (h >= 2) {
                                topOffset += canvas.getHeight() / 2 + textHeight * 3;
                            }
                            canvas.drawText(title, leftPos + leftOffset, textTitleBounds.height() + topOffset, textLabelPaint);
                        }
                    }
                }
            } else {
                final List<GraphSection> sections = graph.getSections();
                for (final GraphSection section : sections) {
                    final List<String> titles = section.getTitles();
                    for (int i = 0; i < titles.size(); i++) {
                        final String title = titles.get(i);
                        final float leftPos = i * circleSize;
                        final Rect textTitleBounds = new Rect();
                        textLabelPaint.getTextBounds(title, 0, title.length(), textTitleBounds);
                        final float leftOffset = (circleSize - textTitleBounds.width()) / 2;
                        canvas.drawText(title, leftPos + leftOffset, textTitleBounds.height(), textLabelPaint);
                    }
                }
            }

            if (cellController != null) {
                for (final ArrayList<GridCellDrawable> cells : cellController) {
                    for (final GridCellDrawable cell : cells) {
                        cell.draw(canvas);
                    }
                }
            }
        }

        /**
         * Called to establish the circle size.  Everything in the {@link GridGraphDrawable} is
         * based on the circleSize.
         */
        private void initHeight(@NonNull final Graph graph) {
            final int elements;
            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                elements = MANY_CELLS;
            } else {
                elements = DEFAULT_CELLS;
            }
            this.circleSize = graphWidth / elements;
            this.padding = circleSize * .08f;
            this.height = getHeight(graph);
            this.reservedTopSpace = textHeight * 2 + padding;
            this.radius = circleSize / 2 - padding * 2;
            updateCellController();
        }

        private void initHeight() {
            initHeight(this.graph);
        }

        /**
         * Should be called each time the graph changes.
         */
        private void updateCellController() {
            cellController = new GridCellController();
            if (graph == null || graph.getSections() == null) {
                return;
            }

            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                final List<Graph> graphs = graph.convertToQuarterGraphs();
                for (int j = 0; j < graphs.size(); j++) {
                    final Graph graph = graphs.get(j);
                    final List<GraphSection> sections = graph.getSections();
                    for (int i = 0; i < sections.size(); i++) {
                        cellController.createSection();

                        final List<Float> values = sections.get(i).getValues();
                        for (int h = 0; h < values.size(); h++) {
                            final Float value = values.get(h);
                            final QuarterCellDrawable cellDrawable = new QuarterCellDrawable(j, i, h, value);
                            cellController.add(cellDrawable);
                        }
                        for (final int indexValue : sections.get(i).getHighlightedValues()) {
                            final QuarterCellDrawable highlightCell = (QuarterCellDrawable) cellController.getCell(indexValue);
                            if (highlightCell != null) {
                                highlightCell.highlightCell();
                            }
                        }
                    }
                }
                return;
            }

            final List<GraphSection> sections = graph.getSections();
            for (int h = 0; h < sections.size(); h++) {
                cellController.createSection();

                final List<Float> values = sections.get(h).getValues();
                for (int i = 0; i < values.size(); i++) {
                    final Float value = values.get(i);
                    final GridCellDrawable cellDrawable = new GridCellDrawable(h, i, value);
                    cellController.add(cellDrawable);
                }

                for (final int indexValue : sections.get(h).getHighlightedValues()) {
                    final GridCellDrawable highlightCell = cellController.getCell(indexValue);
                    if (highlightCell != null) {
                        highlightCell.highlightCell();
                    }
                }
            }
        }

        private int getHeight(@NonNull final Graph graph) {
            if (graph.getTimeScale() == Trends.TimeScale.LAST_3_MONTHS) {
                final float sections = graph.getQuarterSections() + 1.5f;
                return (int) (sections * (circleSize + padding) - padding) + textHeight * 2;

            }
            return (int) (graph.getSections().size() * (circleSize + padding) - padding);
        }

        /**
         * Class to store {@link GridTrendGraphView.GridGraphDrawable.GridCellDrawable}
         * in a way that reflects their arrangement in a given {@link Graph}.
         * Has methods to quickly perform repetitive actions.
         */
        private class GridCellController extends ArrayList<ArrayList<GridCellDrawable>> {

            private void createSection() {
                add(new ArrayList<>());
            }

            public void add(final int section, @NonNull final GridCellDrawable cell) {
                get(section).add(cell);
            }

            public void add(@NonNull final GridCellDrawable cell) {
                if (size() <= 0) {
                    return;
                }
                get(size() - 1).add(cell);
            }

            private GridCellDrawable getCell(final int i) {
                if (size() <= 0) {
                    return null;
                }
                if (get(size() - 1).size() <= i) {
                    return null;
                }
                return get(size() - 1).get(i);
            }
        }

        private class QuarterCellDrawable extends GridCellDrawable {
            private final int graphNumber;
            private final float height;

            private QuarterCellDrawable(final int graphNumber, final int sectionIndex, final int index, @Nullable final Float value) {
                super(sectionIndex, index, value);
                this.graphNumber = graphNumber;
                this.left = index * circleSize + padding + (circleSize - padding - padding) / 2;
                if (graphNumber > 1) {
                    height = ((graph.getQuarterSections() / 2 - sectionIndex) * (padding + circleSize)) + radius - textHeight * 2;
                } else {
                    height = ((graph.getQuarterSections() - sectionIndex + 1) * (padding + circleSize)) + radius + textHeight;
                }
            }

            @Override
            public void draw(@NonNull final Canvas canvas) {
                if (!shouldDraw) {
                    return;
                }
                final float leftOffset;
                if (graphNumber % 2 == 0) {
                    leftOffset = 0;
                } else {
                    leftOffset = (circleSize + canvas.getWidth()) / 2;
                }
                final float left = this.left + leftOffset;
                float top = canvas.getHeight() - height;
                if (top < reservedTopSpace + radius) {
                    top = reservedTopSpace + radius;
                }
                if (top + radius > canvas.getHeight()) {
                    return;
                }

                if (highlight) {
                    canvas.drawCircle(left, top, radius, paint);
                    canvas.drawCircle(left, top, radius - inset, cardPaint);
                    canvas.drawCircle(left, top, radius - inset * 2, paint);
                } else {
                    canvas.drawCircle(left, top, radius, borderPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, emptyCellPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, paint);
                }
            }
        }

        /**
         * Each individual cell that needs to be drawn. This object lets us break apart the drawing
         * and can be used in the future to animate movement from week/month -> quarter view.
         */
        private class GridCellDrawable extends Drawable {
            /**
             * Index number of the {@link GraphSection} this cell belongs to in the {@link Graph}.
             * Used to determine y location of cell.
             */
            protected final int sectionIndex;
            protected float left;
            protected final String textValue;
            protected final Rect textBounds = new Rect();
            protected final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            protected final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            protected boolean highlight = false;
            protected final boolean shouldDraw;

            public GridCellDrawable(final int sectionIndex, final int index, @Nullable final Float value) {
                this.sectionIndex = sectionIndex;
                this.left = index * circleSize + padding + (circleSize - padding - padding) / 2;
                if (value != null) {
                    if (value < 0f) {
                        textValue = context.getString(R.string.missing_data_placeholder);
                        paint.setColor(ContextCompat.getColor(context, R.color.trends_grid_graph_cell_missing));
                        borderPaint.setColor(ContextCompat.getColor(context, R.color.trends_grid_graph_cell_empty_border));
                        shouldDraw = GraphSection.canShow(value);
                    } else {
                        shouldDraw = true;
                        textValue = Styles.createTextValue(value, 0);
                        final Condition condition = graph.getConditionForValue(value);
                        paint.setColor(ContextCompat.getColor(context, condition.colorRes));
                        borderPaint.setColor(ContextCompat.getColor(context, condition.colorRes));

                    }
                } else {
                    shouldDraw = true;
                    textValue = "";
                    paint.setColor(emptyCellPaint.getColor());
                    borderPaint.setColor(ContextCompat.getColor(context, R.color.trends_grid_graph_cell_empty_border));
                }

                textCellPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);
            }

            @Override
            public void draw(@NonNull final Canvas canvas) {
                if (!shouldDraw) {
                    return;
                }
                float top = canvas.getHeight() - ((graph.getSections().size() - sectionIndex) * (padding + circleSize)) + radius;
                if (top < reservedTopSpace + radius) {
                    top = reservedTopSpace + radius;
                }
                if (top + radius > canvas.getHeight()) {
                    return;
                }

                if (highlight) {
                    canvas.drawCircle(left, top, radius, paint);
                    canvas.drawCircle(left, top, radius - inset, cardPaint);
                    canvas.drawCircle(left, top, radius - inset * 2, paint);
                } else {
                    canvas.drawCircle(left, top, radius, borderPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, emptyCellPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, paint);
                }
                if (showText) {
                    canvas.drawText(textValue, left - textBounds.width() / 2, top + textBounds.height() / 2, textCellPaint);
                }


            }

            @Override
            public void setAlpha(final int alpha) {

            }

            @Override
            public void setColorFilter(final ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }

            protected void highlightCell() {
                highlight = true;
            }
        }
    }


}
