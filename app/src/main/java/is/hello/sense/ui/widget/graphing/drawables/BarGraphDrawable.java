package is.hello.sense.ui.widget.graphing.drawables;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.api.model.v2.GraphSection;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

public class BarGraphDrawable extends TrendGraphDrawable {

    public static final String HOUR_SYMBOL = "h";

    /**
     * Max bar height minus min bar height. Accounts for scalable portion of each bar.
     */
    private final int barHeightDifference;

    /**
     * The height of the rectangles containing max/min values that appear above bars.
     */
    private final int highlightValueHeight;

    /**
     * Left & right padding for the max/min values.
     */
    private final int highlightValueSidePadding;

    /**
     * Space between a max/min value bubble and the bar below it.
     */
    private final int highlightBottomMargin;

    /**
     * Space used by calendar labels text, and the maxes highlight bubble. Used to determine the
     * highest Y position value any bar can possibly have.
     */
    private final float graphTopSpace;

    /**
     * Height of graph. Sum of all required margins, text, and expected graph heights.
     */
    private final int totalGraphHeight;

    /**
     * Will determine the width and space each bar should consume based on the canvas and graph type.
     */

    private final CanvasValues canvasValues = new CanvasValues();

    private final RectF barBoundsRect = new RectF();
    private final RectF highlightBounds = new RectF();
    private final RectF highlightTextBounds = new RectF();
    private final Rect textBounds = new Rect();


    private final Path drawingPath = new Path();

    private final TextPaint highlightTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    public BarGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
        super(context, graph, animatorContext);

        Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
        Drawing.updateTextPaintFromStyle(highlightTextPaint, context, R.style.AppTheme_Text_Trends_BarGraph_HighLight);


        this.highlightPaint.setColor(ContextCompat.getColor(context, R.color.trends_bargraph_hightlight));
        this.barHighlightPaint.setColor(ContextCompat.getColor(context, R.color.trends_bargraph_hightlight_bar));
        this.barPaint.setColor(ContextCompat.getColor(context, R.color.trends_bargraph_bar));


        final int dashedLineLength = resources.getDimensionPixelSize(R.dimen.trends_bargraph_dashed_line_length);
        this.dashedLinePaint.setColor(ContextCompat.getColor(context, R.color.grey));
        this.dashedLinePaint.setStyle(Paint.Style.STROKE);
        this.dashedLinePaint.setStrokeWidth(1);
        this.dashedLinePaint.setPathEffect(new DashPathEffect(new float[]{dashedLineLength, dashedLineLength * 2}, 0));


        this.highlightValueSidePadding = resources.getDimensionPixelSize(R.dimen.gap_xsmall);
        this.highlightValueHeight = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_value_height);
        this.highlightBottomMargin = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_bottom_margin);
        final int highlightTopMargin = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_top_margin);


        final int textHeight = Drawing.getEstimatedLineHeight(textLabelPaint, false);

        this.graphTopSpace = textHeight + highlightTopMargin + highlightBottomMargin + highlightValueHeight;


        final int maxHeight = resources.getDimensionPixelSize(R.dimen.trends_bargraph_max_height);
        final int minHeight = resources.getDimensionPixelSize(R.dimen.trends_bargraph_min_height);
        this.barHeightDifference = maxHeight - minHeight;


        this.totalGraphHeight = maxHeight + textHeight + highlightBottomMargin + highlightTopMargin + highlightValueHeight;

    }

    @Override
    public int getIntrinsicHeight() {
        return totalGraphHeight;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        canvasValues.updateValues(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        int leftSpace = 0;
        final ArrayList<RectF> highlightedColumns = new ArrayList<>();
        final ArrayList<DrawingText> highlightedTexts = new ArrayList<>();
        final ArrayList<Path> highlightedBubbles = new ArrayList<>();

        List<GraphSection> sections = graph.getSections();
        for (int i = 0; i < sections.size(); i++) {
            boolean hideLastSection = false;
            GraphSection graphSection = sections.get(i);
            // Draw Text Labels
            List<String> titles = graphSection.getTitles();
            for (int j = 0; j < titles.size(); j++) {
                String title = titles.get(j).toUpperCase();
                calculateTitleTextBounds(j, leftSpace, title, textBounds);
                if ( textBounds.left + textBounds.width() + 10 > graphSection.getValues().size() * (canvasValues.barWidth + canvasValues.barSpace)){
                    continue;
                }else if (textBounds.left + textBounds.width() + 10 < canvas.getWidth()) {
                    canvas.drawText(title, textBounds.left, textBounds.top, textLabelPaint);
                } else if (i == sections.size() - 1) {
                    hideLastSection = true;
                }
            }

            // Draw Bars
            List<Float> values = graphSection.getValues();
            for (int j = 0; j < values.size(); j++) {
                if (values.get(j) == -1) {
                    continue;
                }
                final float scaledRatio = (values.get(j) - graph.getMinValue()) / (graph.getMaxValue() - graph.getMinValue());
                calculateBarBounds(j, scaledRatio, leftSpace, barBoundsRect);
                canvas.drawRect(barBoundsRect, barPaint);
            }
            // Draw Dashed Line
            if (leftSpace > 0 && !hideLastSection ) {
                calculateDashedLinePath(leftSpace, drawingPath);
                canvas.drawPath(drawingPath, dashedLinePaint);
            }
            leftSpace += values.size() * (canvasValues.barSpace + canvasValues.barWidth);
        }
        leftSpace = 0;
        // Go through each section again for highlighted bars. If we draw these in the first run and one of the last bars is highlighted,
        // we risk the dashed line and first bar in the next section being drawn over the floating min/max bubble.
        for (int i = 0; i < sections.size(); i++) {
            GraphSection graphSection = sections.get(i);
            List<Float> values = graphSection.getValues();

            // Draw Highlighted Bars
            for (int j = 0; j < graphSection.getHighlightedValues().size(); j++) {
                int highlightedIndex = graphSection.getHighlightedValues().get(j);
                final float value = values.get(highlightedIndex);
                final float scaledRatio = (value - graph.getMinValue()) / (graph.getMaxValue() - graph.getMinValue());

                final String textValue = Styles.createTextValue(value) + HOUR_SYMBOL;
                highlightTextPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);

                calculateBarBounds(highlightedIndex, scaledRatio, leftSpace, barBoundsRect);
                calculateHighlightBounds(textBounds.centerX(), barBoundsRect, highlightBounds); // This is the min / max bubble's position above the bar.
                calculateHighlightTextBounds(textBounds, highlightBounds, highlightTextBounds);
                drawingPath.reset();
                drawingPath.addRoundRect(highlightBounds, resources.getDimensionPixelSize(R.dimen.raised_item_corner_radius), resources.getDimensionPixelSize(R.dimen.raised_item_corner_radius),
                                         Path.Direction.CW);
                highlightedColumns.add(new RectF(barBoundsRect));
                highlightedBubbles.add(new Path(drawingPath));
                highlightedTexts.add(new DrawingText(textValue, new RectF(highlightTextBounds)));
            }

            leftSpace += values.size() * (canvasValues.barSpace + canvasValues.barWidth);
        }
        // highlight the bar
        for (RectF column : highlightedColumns) {
            canvas.drawRect(column, barHighlightPaint);
        }
        // draw the min/max bubble above it.
        for (Path path : highlightedBubbles) {
            canvas.drawPath(path, highlightPaint);
        }
        // draw the text in that min/max bubble.
        for (DrawingText drawingText : highlightedTexts) {
            canvas.drawText(drawingText.text, drawingText.bounds.left, drawingText.bounds.top, highlightTextPaint);
        }
    }

    class DrawingText {
        @NonNull
        private final String text;
        @NonNull
        private final RectF bounds;

        public DrawingText(@NonNull String text, @NonNull RectF bounds) {
            this.text = text;
            this.bounds = bounds;
        }
    }

    @Override
    public void updateGraph(@NonNull Graph graph) {
        if (graph.getTimeScale() == this.graph.getTimeScale()) {
            //        return; //todo uncomment when done testing.
        }
        ValueAnimator animator = ValueAnimator.ofFloat(maxScaleFactor, minScaleFactor);
        animator.setDuration(Anime.DURATION_NORMAL);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            setScaleFactor((float) a.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                BarGraphDrawable.this.graph = graph;
                canvasValues.updateValues(getBounds());
                showGraphAnimation();
            }
        });
        animatorContext.startWhenIdle(animator);
    }

    private float getLeftPosition(float index, int leftSpace) {
        return leftSpace + index * (canvasValues.barSpace + canvasValues.barWidth);
    }

    /**
     * @param barIndex  index position of the bar value from the list of values.
     * @param ratio     value of the bar compared to the maximum value contained.
     * @param leftSpace space to offset from left.
     * @param outRect   Position of bar.
     */
    public void calculateBarBounds(int barIndex, float ratio, int leftSpace, @NonNull RectF outRect) {
        final float left = getLeftPosition(barIndex, leftSpace);
        final float top = canvasValues.bottom - (canvasValues.bottom - (graphTopSpace + barHeightDifference - (barHeightDifference * ratio))) * valueScaleFactor;
        outRect.left = left;
        outRect.top = top;
        outRect.right = left + canvasValues.barWidth;
        outRect.bottom = canvasValues.bottom;
    }

    /**
     * @param titleIndex index position of the title from the list of title.
     * @param leftSpace  space to offset from left.
     * @param text       text to display.
     * @param outRect    Boundry of space the given text will take.
     */
    private void calculateTitleTextBounds(int titleIndex, int leftSpace, String text, @NonNull Rect outRect) {
        textLabelPaint.getTextBounds(text, 0, text.length(), outRect);
        float left = getLeftPosition(titleIndex, leftSpace);
        if (canvasValues.barWidth > outRect.width()) {
            left += (canvasValues.barWidth - outRect.width()) / 2;
        } else {
            left += canvasValues.barWidth;
        }
        outRect.left = (int) left;
        outRect.right = (int) left + outRect.width();
        outRect.top = outRect.height();
    }

    /**
     * @param leftSpace space to offset from left.
     * @param outPath   Path to apply position of dashed line to.
     */
    private void calculateDashedLinePath(int leftSpace, @NonNull Path outPath) {
        outPath.reset();
        outPath.moveTo(leftSpace - canvasValues.barSpace / 2, 0);
        outPath.lineTo(leftSpace - canvasValues.barSpace / 2, canvasValues.bottom);
    }

    /**
     * @param textCenterX center of text x position.
     * @param barBounds   bounds of bar being highlighted.
     * @param outRect     Min/max bubbles position above given barBounds.
     */
    private void calculateHighlightBounds(int textCenterX, @NonNull RectF barBounds, @NonNull RectF outRect) {
        outRect.left = barBounds.centerX() - textCenterX - highlightValueSidePadding;
        outRect.right = barBounds.centerX() + textCenterX + highlightValueSidePadding;
        outRect.top = barBounds.top - highlightValueHeight - highlightBottomMargin;
        outRect.bottom = outRect.top + highlightValueHeight;

        if (outRect.left < 0) {
            outRect.offset(Math.abs(outRect.left), 0);
        }
    }

    /**
     * @param textBounds      text bounds of the min/max text values.
     * @param highlightBounds bounds to center text inside of.
     * @param outRect         Position to draw min/max text at.
     */
    private void calculateHighlightTextBounds(@NonNull Rect textBounds, @NonNull RectF highlightBounds, @NonNull RectF outRect) {
        outRect.set(highlightBounds);
        float offset = (highlightBounds.width() - textBounds.width()) / 2;
        outRect.offset(offset, 0);
        outRect.top = highlightBounds.centerY() + textBounds.height() / 2;
    }

    private class CanvasValues {

        public float barSpace;
        public float barWidth;
        public float bottom;

        public void updateValues(Rect rect) {
            this.barSpace = rect.width() * Styles.getBarSpacePercent(graph.getTimeScale());
            this.barWidth = rect.width() * Styles.getBarWidthPercent(graph.getTimeScale());
            this.bottom = rect.height();
        }

    }

}
