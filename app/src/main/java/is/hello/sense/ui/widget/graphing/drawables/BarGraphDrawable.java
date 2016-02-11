package is.hello.sense.ui.widget.graphing.drawables;

import android.animation.Animator;
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

    private CanvasValues canvasValues;
    /**
     * Track how far from the left we should draw from.
     */
    private float leftSpace;


    private final TextPaint highlightTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint barHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint dashedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);


    public BarGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
        super(graph, context, animatorContext);

        Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
        Drawing.updateTextPaintFromStyle(highlightTextPaint, context, R.style.AppTheme_Text_Trends_BarGraph_HighLight);


        this.barHighlightPaint.setColor(ContextCompat.getColor(context, R.color.trends_bargraph_hightlight_bar_color));
        this.barPaint.setColor(ContextCompat.getColor(context, R.color.trends_bargraph_bar_color));


        final int dashedLineLength = resources.getDimensionPixelSize(R.dimen.trends_bargraph_dashed_line_length);
        this.dashedLinePaint.setARGB(255, 0, 0, 0);
        this.dashedLinePaint.setStyle(Paint.Style.STROKE);
        this.dashedLinePaint.setStrokeWidth(1);
        this.dashedLinePaint.setPathEffect(new DashPathEffect(new float[]{dashedLineLength, dashedLineLength * 2}, 0));


        this.highlightValueSidePadding = resources.getDimensionPixelSize(R.dimen.gap_xsmall);
        this.highlightValueHeight = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_value_height);
        this.highlightBottomMargin = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_bottom_margin);
        final int highlightTopMargin = resources.getDimensionPixelSize(R.dimen.trends_bargraph_highlight_top_margin);


        final Rect bounds = new Rect();
        textLabelPaint.getTextBounds("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 0, 26, bounds);
        final int textHeight = bounds.height();

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
    public void draw(Canvas canvas) {

        canvasValues = new CanvasValues(canvas);
        leftSpace = 0;
        List<GraphSection> sections = graph.getSections();
        for (int i = 0; i < sections.size(); i++) {
            boolean hideLastSection = false;
            GraphSection graphSection = sections.get(i);
            // Draw Text Labels
            List<String> titles = graphSection.getTitles();
            for (int j = 0; j < titles.size(); j++) {
                RectF textPositionBounds = getTextBounds(j, titles.get(j).toUpperCase());
                if (textPositionBounds.left + textPositionBounds.width() + 5 < canvas.getWidth()) {
                    canvas.drawText(titles.get(j).toUpperCase(), textPositionBounds.left, textPositionBounds.top, textLabelPaint);
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
                canvas.drawRect(getBarBounds(j, scaledRatio), barPaint);
            }
            // Draw Dashed Line
            if (leftSpace > 0 && !hideLastSection) {
                canvas.drawPath(getDashedLinePath(), dashedLinePaint);
            }
            leftSpace += values.size() * canvasValues.barSpace + values.size() * canvasValues.barWidth;
        }
        leftSpace = 0;
        // Go through each section again for highlighted bars. If we draw these in the first run and one of the last bars is highlighted,
        // we risk the dashed line and first bar in the next section being drawn over the floating min/max bubble.
        for (int i = 0; i < sections.size(); i++) {
            GraphSection graphSection = sections.get(i);
            List<Float> values = graphSection.getValues();

            // Draw Highlighted Bars
            for (int highlightedIndex : graphSection.getHighlightedValues()) {
                final float value = values.get(highlightedIndex);
                final float scaledRatio = (value - graph.getMinValue()) / (graph.getMaxValue() - graph.getMinValue());

                final String textValue = Styles.createTextValue(value) + HOUR_SYMBOL;
                final Rect textBounds = new Rect();
                highlightTextPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);

                final RectF barBounds = getBarBounds(highlightedIndex, scaledRatio);
                final RectF highlightBounds = getHighlightBounds(textBounds, barBounds); // This is the min / max bubble's position above the bar.
                final RectF highlightTextBounds = getHighlightTextBounds(textBounds, highlightBounds);
                Path highlightBoundsPath = new Path();
                highlightBoundsPath.addRoundRect(highlightBounds, 15f, 15f,
                                                 Path.Direction.CW);
                // highlight the bar
                canvas.drawRect(barBounds, barHighlightPaint);
                // draw the min/max bubble above it.
                canvas.drawPath(highlightBoundsPath, barHighlightPaint);
                // draw the text in that min/max bubble.
                canvas.drawText(textValue, highlightTextBounds.left, highlightTextBounds.top, highlightTextPaint);
            }


            leftSpace += values.size() * canvasValues.barSpace + values.size() * canvasValues.barWidth;
        }
    }

    @Override
    public void updateGraph(@NonNull Graph graph) {
        if (graph.getTimeScale() == this.graph.getTimeScale()) {
            //        return; //todo uncomment when done testing.
        }
        ValueAnimator animator = ValueAnimator.ofFloat(scaleFactorBackward);
        animator.setDuration(Anime.DURATION_NORMAL);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            setScaleFactor((float) a.getAnimatedValue());
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                BarGraphDrawable.this.graph = graph;
                showGraphAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorContext.startWhenIdle(animator);
    }

    private float getLeftPosition(float index) {
        return leftSpace + index * canvasValues.barSpace + index * canvasValues.barWidth;
    }

    /**
     * @param barIndex index position of the bar value from the list of values.
     * @param ratio    value of the bar compared to the maximum value contained.
     * @return Boundary to draw for the given bar.
     */
    public RectF getBarBounds(int barIndex, float ratio) {
        final float left = getLeftPosition(barIndex);
        final float top = canvasValues.bottom - (canvasValues.bottom - (graphTopSpace + barHeightDifference - (barHeightDifference * ratio))) * valueScaleFactor;
        return new RectF(left,
                         top,
                         left + canvasValues.barWidth,
                         canvasValues.bottom
        );
    }

    /**
     * @param titleIndex index position of the title from the list of title.
     * @param text       text to display.
     * @return Boundary of space the given text will take.
     */
    private RectF getTextBounds(int titleIndex, String text) {
        Rect textBounds = new Rect();
        textLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        float left = getLeftPosition(titleIndex);
        if (canvasValues.barWidth > textBounds.width()) {
            left += (canvasValues.barWidth - textBounds.width()) / 2;
        } else {
            left += canvasValues.barWidth;
        }
        RectF rectF = new RectF();
        rectF.left = left;
        rectF.right = left + textBounds.width();
        rectF.top = textBounds.height();
        rectF.inset(Math.abs(textBounds.width() - rectF.width()) / 2, 0);
        return rectF;
    }

    /**
     * @return Path from top to bottom of canvas. Will take into account leftSpace.
     */
    private Path getDashedLinePath() {
        Path path = new Path();
        path.moveTo(leftSpace - canvasValues.barSpace / 2, 0);
        path.lineTo(leftSpace - canvasValues.barSpace / 2, canvasValues.bottom);
        return path;
    }

    /**
     * @param textBounds text bounds of the min/max text values
     * @param barBounds  bounds of bar being highlighted.
     * @return bounds centered above the bar being highlighted for min/max bubble.
     */
    private RectF getHighlightBounds(@NonNull Rect textBounds, @NonNull RectF barBounds) {
        RectF highlightBounds = new RectF();
        highlightBounds.left = barBounds.centerX() - textBounds.width() / 2 - highlightValueSidePadding;
        highlightBounds.right = barBounds.centerX() + textBounds.width() / 2 + highlightValueSidePadding;
        highlightBounds.top = barBounds.top - highlightValueHeight - highlightBottomMargin;
        highlightBounds.bottom = highlightBounds.top + highlightValueHeight;

        if (highlightBounds.left < 0) {
            highlightBounds.offset(Math.abs(highlightBounds.left), 0);
        }
        return highlightBounds;
    }

    /**
     * @param textBounds      text bounds of the min/max text values.
     * @param highlightBounds bounds to center text inside of.
     * @return bounds with positions to draw the text at.
     */
    private RectF getHighlightTextBounds(@NonNull Rect textBounds, @NonNull RectF highlightBounds) {
        RectF highlightTextBounds = new RectF(highlightBounds);
        float offset = (highlightBounds.width() - textBounds.width()) / 2;
        highlightTextBounds.offset(offset, 0);
        highlightTextBounds.top = highlightBounds.centerY() + textBounds.height() / 2;
        return highlightTextBounds;
    }

    private class CanvasValues {

        public final float barSpace;
        public final float barWidth;
        public final float bottom;

        public CanvasValues(Canvas canvas) {
            this.barSpace = canvas.getWidth() * Styles.getBarSpacePercent(graph.getTimeScale());
            this.barWidth = canvas.getWidth() * Styles.getBarWidthPercent(graph.getTimeScale());
            this.bottom = canvas.getHeight();
        }

    }

}
