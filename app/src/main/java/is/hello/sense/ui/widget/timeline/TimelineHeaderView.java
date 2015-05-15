package is.hello.sense.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class TimelineHeaderView extends RelativeLayout implements TimelineFadeItemAnimator.Listener {
    public static final int NULL_SCORE = -1;


    private final Paint dividerPaint = new Paint();
    private final int dividerHeight;


    private final View topFadeView;
    private View scoreContainer;
    private final SleepScoreDrawable scoreDrawable;
    private final TextView scoreText;
    private final TextView messageText;

    private final int backgroundColor;
    private final int messageTextColor;

    private boolean firstTimeline;
    private AnimatorContext animatorContext;
    private @Nullable ValueAnimator colorAnimator;


    //region Lifecycle

    public TimelineHeaderView(@NonNull Context context) {
        this(context, null);
    }

    public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Resources resources = getResources();
        int dividerColor = resources.getColor(R.color.timeline_header_border);
        dividerPaint.setColor(dividerColor);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);


        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_header, this, true);

        this.topFadeView = findViewById(R.id.view_timeline_header_fade);

        this.scoreContainer = findViewById(R.id.view_timeline_header_chart);
        this.scoreDrawable = new SleepScoreDrawable(getResources(), true);
        scoreContainer.setBackground(scoreDrawable);

        this.scoreText = (TextView) findViewById(R.id.view_timeline_header_chart_score);
        this.messageText = (TextView) findViewById(R.id.view_timeline_header_chart_message);
        messageText.setAlpha(0f);
        Views.makeTextViewLinksClickable(messageText);

        this.backgroundColor = resources.getColor(R.color.background_timeline);
        this.messageTextColor = messageText.getCurrentTextColor();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE && colorAnimator != null) {
            colorAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int right = canvas.getWidth(),
            bottom = canvas.getHeight();

        canvas.drawRect(0, bottom - dividerHeight,
                right, bottom, dividerPaint);
    }

    //endregion


    //region Attributes


    public void setFirstTimeline(boolean firstTimeline) {
        this.firstTimeline = firstTimeline;

        if (firstTimeline) {
            scoreContainer.setTranslationY(getResources().getDimensionPixelSize(R.dimen.gap_large));
            scoreContainer.setAlpha(0f);
        } else {
            scoreContainer.setTranslationY(0f);
            scoreContainer.setAlpha(1f);
        }
    }

    public void setAnimatorContext(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setChildFadeAmount(float amount) {
        if (amount <= 0.5f) {
            // messageText has rich text, setAlpha is too expensive.
            float messageFadeAmount = amount / 0.5f;
            messageText.setTextColor(Drawing.interpolateColors(messageFadeAmount, backgroundColor, messageTextColor));
        } else {
            messageText.setTextColor(messageTextColor);
        }

        topFadeView.setTranslationY(-getTop());
    }

    //endregion


    //region Scores

    private void setScore(int score) {
        int color;
        if (score < 0) {
            color = getResources().getColor(R.color.sensor_unknown);

            scoreDrawable.setValue(0);
            scoreText.setText(R.string.missing_data_placeholder);

            setWillNotDraw(true);
        } else {
            color = Styles.getSleepScoreColor(getContext(), score);

            scoreDrawable.setValue(score);
            scoreText.setText(Integer.toString(score));

            setWillNotDraw(false);
        }

        scoreDrawable.setFillColor(color);
        scoreText.setTextColor(color);
    }

    private void startFirstScoreAnimation(int score, @NonNull Runnable fireAdapterAnimations) {
        PropertyAnimatorProxy.animate(scoreContainer, animatorContext)
                .setDuration(Animation.DURATION_SLOW)
                .fadeIn()
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        animateToScore(score, fireAdapterAnimations);
                    }
                })
                .startWhenIdle();
    }

    private void animateToScore(int score, @NonNull Runnable fireAdapterAnimations) {
        if (score < 0) {
            setScore(score);
            fireAdapterAnimations.run();
        } else {
            setWillNotDraw(false);

            if (colorAnimator != null) {
                colorAnimator.cancel();
            }

            this.colorAnimator = ValueAnimator.ofInt(scoreDrawable.getValue(), score);
            if (firstTimeline) {
                colorAnimator.setDuration(Animation.DURATION_NORMAL);
            } else {
                colorAnimator.setDuration(Animation.DURATION_SLOW);
            }
            colorAnimator.setInterpolator(Animation.INTERPOLATOR_DEFAULT);

            int startColor = Styles.getSleepScoreColor(getContext(), scoreDrawable.getValue());
            int endColor = Styles.getSleepScoreColor(getContext(), score);
            colorAnimator.addUpdateListener(a -> {
                Integer newScore = (Integer) a.getAnimatedValue();
                int color = Drawing.interpolateColors(a.getAnimatedFraction(), startColor, endColor);

                scoreDrawable.setValue(newScore);
                scoreDrawable.setFillColor(color);

                scoreText.setText(newScore.toString());
                scoreText.setTextColor(color);
            });
            colorAnimator.addListener(new AnimatorListenerAdapter() {
                boolean wasCanceled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    scoreDrawable.setValue(score);
                    scoreDrawable.setFillColor(endColor);

                    scoreText.setText(Integer.toString(score));
                    scoreText.setTextColor(endColor);

                    setFirstTimeline(false);

                    this.wasCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (colorAnimator == animation) {
                        TimelineHeaderView.this.colorAnimator = null;

                        if (!wasCanceled && firstTimeline) {
                            completeFirstScoreAnimation(fireAdapterAnimations);
                        }
                    }
                }
            });

            colorAnimator.addListener(animatorContext);
            animatorContext.runWhenIdle(colorAnimator::start);
        }
    }

    private void completeFirstScoreAnimation(@NonNull Runnable fireAdapterAnimations) {
        animatorContext.transaction(f -> {
            f.animate(scoreContainer)
             .translationY(0f);

            f.animate(messageText)
             .fadeIn();
        }, finished -> {
            if (finished) {
                fireAdapterAnimations.run();
            }
        });
    }

    //endregion


    //region Binding

    public void bindMessage(@Nullable CharSequence message) {
        messageText.setText(message);
    }

    public void bindScore(int score, @NonNull Runnable fireAdapterAnimations) {
        if (firstTimeline) {
            startFirstScoreAnimation(score, fireAdapterAnimations);
        } else {
            fireAdapterAnimations.run();
            animateToScore(score, fireAdapterAnimations);
        }
    }

    public void bindError(@NonNull Throwable e) {
        setScore(NULL_SCORE);
        messageText.setText(getResources().getString(R.string.timeline_error_message, e.getMessage()));
    }

    //endregion


    //region Timeline Animations

    @Override
    public void onTimelineAnimationWillStart(@NonNull AnimatorContext animatorContext, @NonNull AnimatorConfig animatorConfig) {
        if (messageText.getAlpha() > 0f) {
            return;
        }

        PropertyAnimatorProxy animator = PropertyAnimatorProxy.animate(messageText, animatorContext);
        animatorConfig.apply(animator);
        animator.fadeIn();
        animator.addOnAnimationCompleted(finished -> {
            if (!finished) {
                messageText.setAlpha(1f);
            }
        });
        animator.startWhenIdle();
    }

    @Override
    public void onTimelineAnimationDidEnd(boolean finished) {
        if (!finished) {
            PropertyAnimatorProxy.stop(messageText);
        }
    }

    //endregion
}
