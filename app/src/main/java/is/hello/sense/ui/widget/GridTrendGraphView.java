package is.hello.sense.ui.widget;

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
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.view.View;
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
import is.hello.sense.ui.widget.graphing.TrendGraphView;
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
        ValueAnimator animator = ValueAnimator.ofFloat(minAnimationFactor, maxAnimationFactor);
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
        if (targetHeight < currentHeight) {
            // The drawable is going to decrease in size, wait for the animation to end before binding a new graph that will remove cells.
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    GridTrendGraphView.super.bindGraph(graph);
                }
            });
        } else {
            // The drawable is increasing in size, add cells immediately so they appear as it's height increases.
            super.bindGraph(graph);
        }

        animatorContext.startWhenIdle(animator);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getCircleSize() == 0) {
            // if circle size is 0, everything will have a height and width of 0.
            // At this point we have an accurate width measurement of the view. So we can scale the graph to fit inside of the width it consumes.
            ((GridGraphDrawable) drawable).initHeight(View.MeasureSpec.getSize(widthMeasureSpec) / 7);
        }

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
     * @param show
     */
    public void showText(boolean show) {
        this.showText = show;
    }


    /**
     * Drawable that represents the Grid of a given Graph Object.
     */
    private class GridGraphDrawable extends TrendGraphDrawable {

        private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textGridPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int textHeight;

        private GridCellController cellController;
        private int height = 0;
        private float circleSize = 0;
        private float padding = 0;
        private float reservedTopSpace = 0;
        private float radius = 0;
        private float minTop = 0;

        public GridGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, graph, animatorContext);
            Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
            Drawing.updateTextPaintFromStyle(textGridPaint, context, R.style.AppTheme_Text_Trends_GridGraph);
            whitePaint.setARGB(255, 255, 255, 255);
            Rect bounds = new Rect();
            textLabelPaint.getTextBounds("A", 0, 1, bounds);
            textHeight = bounds.height();
        }

        @Override
        public int getIntrinsicHeight() {
            return height + textHeight * 2 + (int) padding;
        }

        @Override
        public void updateGraph(@NonNull Graph graph) {
            this.graph = graph;
            this.minTop = minPossibleTop();
            updateCellController();
            invalidate();
        }

        @Override
        public void draw(Canvas canvas) {
            // Draw Text Labels
            List<GraphSection> sections = graph.getSections();
            for (GraphSection section : sections) {
                List<String> titles = section.getTitles();
                for (int i = 0; i < titles.size(); i++) {
                    final String title = titles.get(i);
                    final float leftSpace = i * circleSize;
                    final Rect textTitleBounds = new Rect();
                    textLabelPaint.getTextBounds(title, 0, title.length(), textTitleBounds);
                    final float offSet = (circleSize - textTitleBounds.width()) / 2;
                    canvas.drawText(title, leftSpace + offSet, textTitleBounds.height(), textLabelPaint);
                }
            }

            for (ArrayList<GridCellDrawable> cells : cellController) {
                for (GridCellDrawable cell : cells) {
                    cell.draw(canvas);
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
            this.circleSize = circleSize;
            this.padding = circleSize * .075f;
            this.height = getHeight(this.graph);
            this.reservedTopSpace = circleSize + padding;
            this.radius = circleSize / 2 - padding * 2;
            updateCellController();

        }

        /**
         * Should be called each time the graph changes.
         */
        private void updateCellController() {
            cellController = new GridCellController();
            List<GraphSection> sections = graph.getSections();
            for (int h = 0; h < sections.size(); h++) {
                cellController.createSection();

                List<Float> values = sections.get(h).getValues();
                for (int i = 0; i < values.size(); i++) {
                    Float value = values.get(i);
                    GridCellDrawable cellDrawable = new GridCellDrawable(h, i, value);
                    cellController.add(cellDrawable);
                }

                for (int indexValue : sections.get(h).getHighlightedValues()) {
                    GridCellDrawable highlightCell = cellController.getCell(indexValue);
                    if (highlightCell != null) {
                        highlightCell.highlightCell();
                    }
                }

            }
        }

        private int getHeight(@NonNull Graph graph) {
            return (int) ((graph.getSections().size()) * circleSize);
        }

        private float minPossibleTop() {
            float spaceFromBottom = 0;
            for (int i = 0; i < graph.getSections().size(); i++) {
                spaceFromBottom += padding + circleSize;
            }
            return spaceFromBottom;
        }

        /**
         * Class to store CellDrawables in a way that reflects their arrangement in a given Graph.
         * Has methods to quickly perform actions.
         */
        private class GridCellController extends ArrayList<ArrayList<GridCellDrawable>> {

            public void createSection() {
                add(new ArrayList<>());
            }

            public void add(int section, GridCellDrawable cell) {
                get(section).add(cell);
            }

            public void add(GridCellDrawable cell) {
                if (size() <= 0) {
                    return;
                }
                get(size() - 1).add(cell);
            }

            public GridCellDrawable getCell(int i) {
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
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            /**
             * Index number of the {@link GraphSection} this cell belongs to in the {@link Graph}.
             * Used to determine y location of cell.
             */
            private final int sectionIndex;
            private final float left;
            private final float textLeft;

            private float top;
            private float textTop;
            private final String textValue;

            /**
             * Horizontal offset position of text.
             */
            private final float hOffset;

            /**
             * Vertical offset position of text.
             */
            private final float vOffset;

            /**
             * True when the cell has a value greater than -1.
             */
            private final boolean hasValue;
            private final Rect textBounds = new Rect();

            private boolean highlight = false;

            public GridCellDrawable(int sectionIndex, int index, Float value) {
                this.sectionIndex = sectionIndex;
                final float leftSpace = index * circleSize;
                this.left = leftSpace + padding + (circleSize - padding - padding) / 2;
                if (value != null) {
                    if (value < 0f) {
                        hasValue = false;
                        textValue = context.getString(R.string.missing_data_placeholder);
                        paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_missing));
                        borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
                    } else {
                        hasValue = true;
                        textValue = Styles.createTextValue(value, 0);
                        final Condition condition = graph.getConditionForValue(value);
                        paint.setColor(ContextCompat.getColor(getContext(), condition.colorRes));
                        borderPaint.setColor(ContextCompat.getColor(context, condition.colorRes));
                    }
                } else {
                    hasValue = false;
                    textValue = "";
                    paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_cell));
                    borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
                }


                textGridPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);
                if (circleSize - padding > textBounds.width()) {
                    hOffset = (circleSize - padding - textBounds.width()) / 2;
                } else {
                    hOffset = (textBounds.width() - (circleSize - padding)) / 2;
                }
                if (circleSize - padding > textBounds.height()) {
                    vOffset = (circleSize - padding - textBounds.height()) / 2;
                } else {
                    vOffset = (textBounds.height() - (circleSize - padding)) / 2;
                }
                textLeft = leftSpace + hOffset + 2;
            }

            @Override
            public void draw(Canvas canvas) {
                top = getTopPosition(canvas.getHeight(), graph.getSections().size());
                if (top < reservedTopSpace - radius) {
                    top = minTop;
                }
                if (top + radius > canvas.getHeight()) {
                    return;
                }

                textTop = top + vOffset / 4;
                if (highlight && hasValue) {
                    int inset = getContext().getResources().getDimensionPixelSize(R.dimen.trends_gridgraph_border_inset);
                    canvas.drawCircle(left, top, radius, paint);
                    canvas.drawCircle(left, top, radius - inset, whitePaint);
                    canvas.drawCircle(left, top, radius - inset * 2, paint);
                } else {
                    canvas.drawCircle(left, top, radius, borderPaint);
                    canvas.drawCircle(left, top, radius - padding / 8, paint);
                }
                if (showText) {
                    canvas.drawText(textValue, textLeft, textTop, textGridPaint);
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


            private float getTopPosition(int maxSize, int maxIndex) {
                float spaceFromBottom = 0;
                for (int i = sectionIndex + 1; i < maxIndex; i++) {
                    spaceFromBottom += padding + circleSize;
                }
                return maxSize - spaceFromBottom - radius;
            }
        }
    }


}
