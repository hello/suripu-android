package is.hello.sense.ui.widget.graphing.drawables;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

public class BubbleGraphDrawable extends TrendGraphDrawable {
    public static final String PERCENT_SYMBOL = "%";

    private static final int LEFT_BUBBLE = 0;
    private static final int CENTER_BUBBLE = 1;
    private static final int RIGHT_BUBBLE = 2;

    /**
     * Space above the text "Light", "Medium", "Deep"
     */
    private final int titleTopMargin;

    /**
     * Smallest size a bubble can be.
     */
    private final int minBubbleHeight;

    /**
     * Height of graph
     */
    private final int totalGraphHeight;

    /**
     * Holds the 3 bubbles.
     */
    private BubbleController currentBubbleController;

    /**
     * Additional space for percent symbol.
     */
    private final int percent_offset;

    private int canvasWidth;
    private int midY;
    private float animationScaleFactor;

    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mediumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint deepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textValuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPercentPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private Rect textValueRect = new Rect();
    private Rect textTitleRect = new Rect();

    public BubbleGraphDrawable(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
        super(context, graph, animatorContext);

        this.minBubbleHeight = resources.getDimensionPixelSize(R.dimen.trends_bubblegraph_min_height);
        this.titleTopMargin = resources.getDimensionPixelSize(R.dimen.trends_bubblegraph_title_top_margin);

        this.lightPaint.setColor(ContextCompat.getColor(context, R.color.trends_bubblegraph_light_bubble));
        this.mediumPaint.setColor(ContextCompat.getColor(context, R.color.trends_bubblegraph_medium_bubble));
        this.deepPaint.setColor(ContextCompat.getColor(context, R.color.trends_bubblegraph_deep_bubble));

        Drawing.updateTextPaintFromStyle(textValuePaint, context, R.style.AppTheme_Text_Trends_BubbleGraph);
        Drawing.updateTextPaintFromStyle(textPercentPaint, context, R.style.AppTheme_Text_Trends_BubbleGraph);
        textPercentPaint.setAlpha(178);
        Drawing.updateTextPaintFromStyle(textTitlePaint, context, R.style.AppTheme_Text_Trends_BubbleGraph_Title);
        this.percent_offset = resources.getDimensionPixelOffset(R.dimen.trends_bubblegraph_percent_offset);
        textTitlePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.text_size_trends_bubblegraph_title));
        this.totalGraphHeight = context.getResources().getDimensionPixelSize(R.dimen.trends_bubblegraph_max_height);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        canvasWidth = bounds.width();
        midY = bounds.height() / 2;
        currentBubbleController = createBubbleController(graph);
    }

    @Override
    public void draw(Canvas canvas) {
        for (BubbleController.Bubble bubble : currentBubbleController.getDrawOrder()) {
            final String textValue = bubble.getTextValue();
            bubble.getTextValuePaint().getTextBounds(textValue, 0, textValue.length(), textValueRect);
            textTitlePaint.getTextBounds(bubble.textTitle, 0, bubble.textTitle.length(), textTitleRect);
            final float height = (midY - (textValueRect.height() + titleTopMargin + textTitleRect.height()) / 2) + textValueRect.height();
            // Bubble
            canvas.drawCircle(bubble.getMidX(),
                              midY,
                              bubble.getRadius() * valueScaleFactor,
                              bubble.paint);
            // Bubble Value
            canvas.drawText(textValue,
                            bubble.getTextStartX(textValueRect) - percent_offset,
                            height,
                            bubble.getTextValuePaint());
            // % Symbol
            canvas.drawText(PERCENT_SYMBOL,
                            bubble.getTextPercentStartX(textValueRect),
                            height - textValueRect.height() / 2,
                            bubble.getTextPercentPaint());

            // Title Of Bubble
            canvas.drawText(bubble.textTitle,
                            bubble.getTextTitleStartX(),
                            height + textTitleRect.height() + titleTopMargin,
                            textTitlePaint);
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return totalGraphHeight;
    }

    @Override
    public void updateGraph(@NonNull Graph graph) {
        if (graph.getTimeScale() == this.graph.getTimeScale()) {
            return;
        }
        final BubbleController animateTo = createBubbleController(graph);
        if (currentBubbleController == null) {
            currentBubbleController = animateTo;
            return;
        }
        currentBubbleController.get(LEFT_BUBBLE).setTargetBubble(animateTo.get(LEFT_BUBBLE));
        currentBubbleController.get(CENTER_BUBBLE).setTargetBubble(animateTo.get(CENTER_BUBBLE));
        currentBubbleController.get(RIGHT_BUBBLE).setTargetBubble(animateTo.get(RIGHT_BUBBLE));

        ValueAnimator animator = ValueAnimator.ofFloat(minAnimationFactor, maxAnimationFactor);
        animator.setDuration(Anime.DURATION_NORMAL);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            animationScaleFactor = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                BubbleGraphDrawable.this.graph = graph;
                currentBubbleController = animateTo;
            }
        });
        animatorContext.startWhenIdle(animator);


    }

    private BubbleController createBubbleController(@NonNull Graph graph) {
        BubbleController bubbleController = new BubbleController();
        final List<Float> values = graph.getSections().get(0).getValues();
        final List<String> titles = graph.getSections().get(0).getTitles();

        for (int j = 0; j < values.size(); j++) {
            float radius = ((midY * 2 - minBubbleHeight) / 2 * values.get(j)) + (minBubbleHeight / 2);
            radius *= .965f; // shrink radius a little so largest possible bubble wont block text on smallest possible
            bubbleController.add(bubbleController.new Bubble(values.get(j), radius, j, titles.get(j)));
        }
        bubbleController.setBubblePositions();
        return bubbleController;

    }

    /**
     * Responsible for tracking and manipulating the three bubbles.
     */
    private class BubbleController extends ArrayList<BubbleController.Bubble> {

        public void setBubblePositions() {
            Bubble leftBubble = get(LEFT_BUBBLE);
            Bubble centerBubble = get(CENTER_BUBBLE);
            Bubble rightBubble = get(RIGHT_BUBBLE);

            // Determine if the bubbles take up more space than the screen provides.
            float bubbleWidth = leftBubble.radius * 2 + centerBubble.radius * 2 + rightBubble.radius * 2;
            float overflow = bubbleWidth - canvasWidth;

            // Put middle bubble directly in center of the screen.
            centerBubble.midX = canvasWidth / 2;
            leftBubble.midX = centerBubble.getLeftEdgeX() - leftBubble.radius;
            rightBubble.midX = centerBubble.getRightEdgeX() + rightBubble.radius;

            // Bubbles take up to much space. Most likely in portrait mode.
            if (overflow > 0) {
                // Move the side bubbles closer together.
                leftBubble.midX += overflow / 2;
                rightBubble.midX -= overflow / 2;
            } else {
                // Bubbles have enough room to be drawn side by side,
                // but lets move them together a little bit for design.
                leftBubble.midX += leftBubble.radius * .45f;
                rightBubble.midX -= rightBubble.radius * .45f;
            }

            // At this point the bubbles are perfectly distanced from one another to fit in the screen,
            // however they may be to far to the left or right.  The following will shift all of them to fit.

            // Is the left bubble off the screen?
            if (leftBubble.getLeftEdgeX() < 0) {
                offsetPositions(Math.abs(leftBubble.getLeftEdgeX()));
            }

            // Is the right bubble off the screen?
            if (rightBubble.getRightEdgeX() > canvasWidth) {
                offsetPositions(canvasWidth - rightBubble.getRightEdgeX());
            }

        }

        private void offsetPositions(float x) {
            for (Bubble bubble : this) {
                bubble.midX += x;
            }
        }


        public List<Bubble> getDrawOrder() {
            ArrayList<Bubble> bubbles = new ArrayList<>();
            bubbles.add(get(LEFT_BUBBLE));
            bubbles.add(get(RIGHT_BUBBLE));
            bubbles.add(get(CENTER_BUBBLE));
            return bubbles;
        }


        public class Bubble {
            /**
             * X position of bubble center.
             */
            private float midX;

            /**
             * Value of bubble.
             */
            private final float value;

            /**
             * Size of radius.
             */
            private final float radius;

            /**
             * Refers to "Light", "Medium", "Deep"
             */
            private final String textTitle;

            private final Rect textTitleRect;
            private final Paint paint;

            /**
             * Give a target bubble that this bubble should animate towards.
             */
            @Nullable
            public Bubble targetBubble = null;

            /**
             * Use getter functions to take into account any animation effects.
             */
            public Bubble(float value, float radius, int position, String textTitle) {
                this.radius = radius;
                this.value = value;
                this.textTitle = textTitle.toUpperCase();

                if (position == LEFT_BUBBLE) {
                    this.paint = lightPaint;
                } else if (position == CENTER_BUBBLE) {
                    this.paint = mediumPaint;
                } else {
                    this.paint = deepPaint;
                }

                textTitleRect = new Rect();
                textTitlePaint.getTextBounds(textTitle, 0, textTitle.length(), textTitleRect);
            }

            public float getMidX() {
                if (targetBubble != null) {
                    if (midX < targetBubble.midX) {
                        return midX + (targetBubble.midX - midX) * animationScaleFactor;
                    } else if (midX > targetBubble.midX) {
                        return midX - (midX - targetBubble.midX) * animationScaleFactor;
                    }
                }
                return midX;
            }

            public float getRadius() {
                if (targetBubble != null) {
                    if (radius < targetBubble.radius) {
                        return radius + (targetBubble.radius - radius) * animationScaleFactor;
                    } else if (radius > targetBubble.radius) {
                        return radius - (radius - targetBubble.radius) * animationScaleFactor;
                    }
                }
                return radius;
            }

            public float getLeftEdgeX() {
                return getMidX() - getRadius();
            }

            public float getRightEdgeX() {
                return getMidX() + getRadius();
            }

            public float getValue() {
                if (targetBubble != null) {
                    if (value < targetBubble.value) {
                        return value + (targetBubble.value - value) * animationScaleFactor;
                    } else if (value > targetBubble.value) {
                        return value - (value - targetBubble.value) * animationScaleFactor;
                    }
                }
                return value;
            }

            public TextPaint getTextValuePaint() {
                textValuePaint.setTextSize(getRadius() / 1.5f);
                return textValuePaint;
            }

            public TextPaint getTextPercentPaint() {
                textPercentPaint.setTextSize(getRadius() / 3f);
                return textPercentPaint;
            }

            public String getTextValue() {
                return Styles.createTextValue(getValue() * 100, 0);
            }


            public float getTextStartX(@NonNull Rect textRect) {
                return getMidX() - textRect.width() / 2;
            }

            public float getTextPercentStartX(@NonNull Rect textRect) {
                return getMidX() + textRect.width() / 2;
            }

            public float getTextTitleStartX() {
                return getMidX() - textTitleRect.width() / 2;
            }

            public void setTargetBubble(@NonNull Bubble targetBubble) {
                this.targetBubble = targetBubble;
            }
        }
    }
}
