package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

/**
 * View to show a graphs Grid type.
 */
@SuppressLint("ViewConstructor")
public class GridTrendGraphView extends TrendGraphView {

    private final AnimatorContext animatorContext;
    private boolean showText = true;

    public GridTrendGraphView(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
        super(context, animatorContext);
        this.animatorContext = animatorContext;
        this.drawable = new GridGraphDrawable(context, graph, animatorContext);
        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawable.showGraphAnimation();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final float circleSize = getWidth() / 7;
                if (getCircleSize() != circleSize) {
                    ((GridGraphDrawable) drawable).initHeight(getWidth() / 7);
                    requestLayout();
                    invalidate();
                }
            }
        });
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        if (getAlpha() == 0) { // No need to animate, the view isn't visible.
            setDrawableHeight(getDrawableHeight(graph));
            requestLayout();
            super.bindGraph(graph);
            return;
        }
        final int currentHeight = getDrawableHeight();
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
            public void onAnimationEnd(Animator animation) {
                super.onAnimationStart(animation);
                if (!GridTrendGraphView.this.getGraph().equals(graph)) {
                    GridTrendGraphView.super.bindGraph(graph);
                }
            }
        });

        animatorContext.startWhenIdle(animator);
    }

    private float getCircleSize() {
        return ((GridGraphDrawable) drawable).circleSize;
    }

    private int getDrawableHeight() {
        return getDrawableHeight(drawable.getGraph());
    }

    private int getDrawableHeight(@NonNull Graph graph) {
        return ((GridGraphDrawable) drawable).getHeight(graph);
    }

    private void setDrawableHeight(int height) {
        ((GridGraphDrawable) drawable).height = height;
    }

    /**
     * Show/Hide the value of each cell.
     *
     * @param show show text.
     */
    public void showText(boolean show) {
        this.showText = show;
    }


    /**
     * Drawable that represents the Grid of a given Graph Object.
     */
    private class GridGraphDrawable extends TrendGraphDrawable {

        private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textCellPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int textHeight;
        private final int inset = getContext().getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_inset);

        private GridCellController cellController;
        private int height = 0;
        private float circleSize = 0;
        private float padding = 0;
        private float reservedTopSpace = 0;
        private float radius = 0;

        public GridGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, graph, animatorContext);
            Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_GridGraph);
            Drawing.updateTextPaintFromStyle(textCellPaint, context, R.style.AppTheme_Text_Trends_GridGraph_Cell);
            whitePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
            final Rect bounds = new Rect();
            textLabelPaint.getTextBounds("A", 0, 1, bounds);
            textHeight = bounds.height();
        }

        @Override
        public int getIntrinsicHeight() {
            return height + (int) reservedTopSpace;
        }

        @Override
        public void updateGraph(@NonNull Graph graph) {
            this.graph = graph;
            updateCellController();
            invalidate();
        }

        @Override
        public void draw(Canvas canvas) {
            // Draw Text Labels
            final List<GraphSection> sections = graph.getSections();
            for (final GraphSection section : sections) {
                final List<String> titles = section.getTitles();
                for (int i = 0; i < titles.size(); i++) {
                    final String title = titles.get(i);
                    final float leftSpace = i * circleSize;
                    final Rect textTitleBounds = new Rect();
                    textLabelPaint.getTextBounds(title, 0, title.length(), textTitleBounds);
                    final float offSet = (circleSize - textTitleBounds.width()) / 2;
                    canvas.drawText(title, leftSpace + offSet, textTitleBounds.height(), textLabelPaint);
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
         * Called once to establish the circle size.  Everything in the {@link GridGraphDrawable} is
         * based on the circleSize.
         *
         * @param circleSize width / 7
         */
        private void initHeight(float circleSize) {
            if (circleSize == this.circleSize || circleSize == 0 || this.circleSize != 0) {
                return;
            }
            this.circleSize = circleSize;
            this.padding = circleSize * .08f;
            this.height = getHeight(this.graph);
            this.reservedTopSpace = textHeight * 2 + padding;
            this.radius = circleSize / 2 - padding * 2;
            updateCellController();

        }

        /**
         * Should be called each time the graph changes.
         */
        private void updateCellController() {
            cellController = new GridCellController();
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

        private int getHeight(@NonNull Graph graph) {
            return (int) (graph.getSections().size() * (circleSize + padding) - padding);
        }

        /**
         * Class to store {@link GridTrendGraphView.GridGraphDrawable.GridCellDrawable}
         * in a way that reflects their arrangement in a given {@link Graph}.
         * Has methods to quickly perform repetitive actions.
         */
        private class GridCellController extends ArrayList<ArrayList<GridCellDrawable>> {

            public void createSection() {
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

            public GridCellDrawable getCell(final int i) {
                if (size() <= 0) {
                    return null;
                }
                return get(size() - 1).get(i);
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
            private final int sectionIndex;
            private final float left;
            private final String textValue;
            private final Rect textBounds = new Rect();
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private boolean highlight = false;
            private final boolean shouldDraw;

            public GridCellDrawable(final int sectionIndex, final int index, @Nullable final Float value) {
                this.sectionIndex = sectionIndex;
                final float leftSpace = index * circleSize;
                this.left = leftSpace + padding + (circleSize - padding - padding) / 2;
                if (value != null) {
                    if (value < 0f) {
                        textValue = context.getString(R.string.missing_data_placeholder);
                        paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_missing));
                        borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
                        shouldDraw = value != -2f;
                    } else {
                        shouldDraw = true;
                        textValue = Styles.createTextValue(value, 0);
                        final Condition condition = graph.getConditionForValue(value);
                        paint.setColor(ContextCompat.getColor(getContext(), condition.colorRes));
                        borderPaint.setColor(ContextCompat.getColor(context, condition.colorRes));

                    }
                } else {
                    shouldDraw = true;
                    textValue = "";
                    paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_cell));
                    borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
                }
                textCellPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);
            }

            @Override
            public void draw(@NonNull final Canvas canvas) {
                if (!shouldDraw) {
                    return;
                }
                float top = getTopPosition(canvas.getHeight());
                if (top < reservedTopSpace + radius) {
                    top = reservedTopSpace + radius;
                }
                if (top + radius > canvas.getHeight()) {
                    return;
                }

                if (highlight) {
                    canvas.drawCircle(left, top, radius, paint);
                    canvas.drawCircle(left, top, radius - inset, whitePaint);
                    canvas.drawCircle(left, top, radius - inset * 2, paint);
                } else {
                    canvas.drawCircle(left, top, radius, borderPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, paint);
                }
                if (showText) {
                    canvas.drawText(textValue, left - textBounds.width() / 2, top + textBounds.height() / 2, textCellPaint);
                }

            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }

            private void highlightCell() {
                highlight = true;
            }

            private float getTopPosition(final int canvasHeight) {
                return canvasHeight - ((graph.getSections().size() - sectionIndex) * (padding + circleSize)) + radius;
            }
        }
    }


}
