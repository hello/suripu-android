package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
        final int currentHeight = getDrawableHeight();
        final int targetHeight = getDrawableHeight(graph);
        ValueAnimator animator = ValueAnimator.ofFloat(.1f, 1f);
        animator.setDuration(Anime.DURATION_SLOW);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            if (targetHeight > currentHeight) {
                setDrawableHeight((int) (currentHeight + (targetHeight - currentHeight) * (float) a.getAnimatedValue()));
            } else {
                setDrawableHeight((int) (targetHeight + (currentHeight - targetHeight) * (1f - (float) a.getAnimatedValue())));
            }
            requestLayout();

        });
        if (targetHeight < currentHeight) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    GridTrendGraphView.super.bindGraph(graph);
                }
            });
        } else {
            super.bindGraph(graph);
        }

        animatorContext.startWhenIdle(animator);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getCircleSize() == 0) {
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
     * Drawable that represents the Grid of a given Graph Object.
     */
    private class GridGraphDrawable extends TrendGraphDrawable {

        private final TextPaint textLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textGridPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textGridNoValuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private int height = 0;
        private float circleSize = 0;
        private float padding = 0;
        private float reservedTopSpace = 0;
        private float radius = 0;
        private final Rect textBounds = new Rect();

        public GridGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, graph, animatorContext);
            Drawing.updateTextPaintFromStyle(textLabelPaint, context, R.style.AppTheme_Text_Trends_BarGraph);
            Drawing.updateTextPaintFromStyle(textGridPaint, context, R.style.AppTheme_Text_Trends_GridGraph);
            Drawing.updateTextPaintFromStyle(textGridNoValuePaint, context, R.style.AppTheme_Text_Trends_GridGraph_NoValue);
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public void updateGraph(@NonNull Graph graph) {
            this.graph = graph;
            invalidate();
        }

        @Override
        public void draw(Canvas canvas) {
            // Draw Text Labels
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
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
            for (int h = 0; h < sections.size(); h++) {
                GraphSection section = sections.get(h);
                List<Float> values = section.getValues();
                for (int i = 0; i < values.size(); i++) {

                    final Float value = values.get(i);
                    final float leftSpace = i * circleSize;
                    final String textValue;
                    final Paint textPaint;
                    if (value != null) {
                        if (value < 0f) {
                            textValue = "- -";
                            paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_missing));
                            textPaint = textGridNoValuePaint;
                        } else {
                            textValue = Styles.createTextValue(value, 0);
                            final Condition condition = graph.getConditionForValue(value);
                            paint.setColor(ContextCompat.getColor(getContext(), condition.colorRes));
                            textPaint = textGridPaint;
                        }
                    } else {
                        textValue = "";
                        paint.setColor(ContextCompat.getColor(getContext(), R.color.graph_grid_empty_cell));
                        textPaint = textGridNoValuePaint;
                    }
                    final float left = leftSpace + padding + (circleSize - padding - padding) / 2;
                    final float top = getTopPosition(canvas.getHeight(), h, sections.size());

                    if (top + radius > canvas.getHeight()) {
                        continue;
                    }

                    canvas.drawCircle(left, top, radius, borderPaint);
                    canvas.drawCircle(left, top, radius - padding / 2, paint);

                    textPaint.getTextBounds(textValue, 0, textValue.length(), textBounds);
                    final float hOffset;
                    if (circleSize - padding > textBounds.width()) {
                        hOffset = (circleSize - padding - textBounds.width()) / 2;
                    } else {
                        hOffset = (textBounds.width() - (circleSize - padding)) / 2;
                    }
                    final float vOffset;
                    if (circleSize - padding > textBounds.height()) {
                        vOffset = (circleSize - padding - textBounds.height()) / 2;
                    } else {
                        vOffset = (textBounds.height() - (circleSize - padding)) / 2;

                    }

                    canvas.drawText(textValue, leftSpace + hOffset + 3, top + vOffset / 4 - 2, textPaint);
                }
            }
        }

        private void initHeight(float circleSize) {
            this.circleSize = circleSize;
            this.padding = circleSize * .05f;
            this.height = getHeight(this.graph);
            this.reservedTopSpace = circleSize + padding;
            this.radius = circleSize / 2 - padding * 2;
        }

        private int getHeight(@NonNull Graph graph) {
            return (int) ((graph.getSections().size() + .5f) * circleSize);
        }

        private float getTopPosition(int maxSize, int index, int maxIndex) {
            float min = minPossibleTop(maxIndex);
            float spaceFromBottom = 0;
            for (int i = index + 1; i < maxIndex; i++) {
                spaceFromBottom += padding + circleSize;
            }
            final float top = maxSize - spaceFromBottom - radius;
            if (top < min) {
                return min;
            }
            return top;
        }

        private float minPossibleTop(int maxIndex) {
            float spaceFromBottom = 0;
            for (int i = 0; i < maxIndex; i++) {
                spaceFromBottom += padding + circleSize;
            }
            return spaceFromBottom;
        }
    }


}
