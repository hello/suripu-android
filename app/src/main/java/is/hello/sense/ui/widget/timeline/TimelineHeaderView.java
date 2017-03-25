package is.hello.sense.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.util.SafeOnClickListener;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class TimelineHeaderView extends RelativeLayout {
    private static final String SCORE_ANIMATOR_NAME = TimelineHeaderView.class.getSimpleName() + "#scoreAnimator";
    private static final String PULSE_ANIMATOR_NAME = TimelineHeaderView.class.getSimpleName() + "#pulseAnimator";

    private final Paint paint = new Paint();

    private final int solidBackgroundColor;
    private final Drawable gradientBackground;
    private int gradientBackgroundAlpha = 0;

    private final View scoreContainer;
    private final SleepScoreDrawable scoreDrawable;
    private final TextView scoreText;

    private final ViewGroup cardContainer;
    private final TextView cardContents;

    private boolean hasAnimated = false;
    private boolean animationEnabled = true;
    private AnimatorContext animatorContext;


    private
    @Nullable
    ValueAnimator scoreAnimator;
    private
    @Nullable
    ValueAnimator pulseAnimator;
    private
    @Nullable
    ValueAnimator backgroundAnimator;


    //region Lifecycle

    public TimelineHeaderView(@NonNull final Context context) {
        this(context, null);
    }

    public TimelineHeaderView(@NonNull final Context context,
                              @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineHeaderView(@NonNull final Context context,
                              @Nullable final AttributeSet attrs,
                              final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        this.solidBackgroundColor = ContextCompat.getColor(context, R.color.timeline_background);
        final int[] gradientColors = {
                solidBackgroundColor,
                solidBackgroundColor,
                solidBackgroundColor,
        };
        this.gradientBackground = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                                       gradientColors);
        gradientBackground.setAlpha(0);


        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_header, this, true);

        this.scoreContainer = findViewById(R.id.view_timeline_header_chart);
        final int scoreTranslation = getResources().getDimensionPixelSize(R.dimen.x6);
        scoreContainer.setTranslationY(scoreTranslation);

        this.scoreDrawable = new SleepScoreDrawable(getResources(), true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            scoreDrawable.setStateful(true);
        }
        scoreContainer.setBackground(scoreDrawable);

        this.scoreText = (TextView) findViewById(R.id.view_timeline_header_chart_score);


        this.cardContainer = (ViewGroup) findViewById(R.id.view_timeline_header_card);
        cardContainer.setVisibility(INVISIBLE);

        this.cardContents = (TextView) cardContainer.findViewById(R.id.view_timeline_header_card_contents);


        setScoreClickEnabled(false);
    }

    @Override
    protected void onVisibilityChanged(@NonNull final View changedView,
                                       final int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        clearAnimation();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        final int right = canvas.getWidth();
        final int bottom = canvas.getHeight();

        if (gradientBackgroundAlpha < 255) {
            paint.setColor(solidBackgroundColor);
            canvas.drawRect(0, 0, right, bottom, paint);
        }

        if (gradientBackgroundAlpha > 0) {
            gradientBackground.setBounds(0, 0, right, bottom);
            gradientBackground.draw(canvas);

            canvas.drawRect(0, bottom,
                            right, bottom, paint);
        }
    }

    //endregion


    //region Attributes

    public void setAnimationEnabled(final boolean animationEnabled) {
        if (!animationEnabled) {
            clearAnimation();
        }

        this.animationEnabled = animationEnabled;
    }

    public void setAnimatorContext(@NonNull final AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setOnScoreClickListener(@Nullable final View.OnClickListener listener) {
        if (listener != null) {
            final SafeOnClickListener wrapper = new SafeOnClickListener(null, listener);
            final boolean clickEnabled = isScoreClickEnabled();
            scoreContainer.setOnClickListener(wrapper);
            cardContainer.setOnClickListener(wrapper);
            setScoreClickEnabled(clickEnabled);
        } else {
            scoreContainer.setOnClickListener(null);
            cardContainer.setOnClickListener(null);
            setScoreClickEnabled(false);
        }
    }

    public void setScoreClickEnabled(final boolean enabled) {
        scoreContainer.setClickable(enabled);
        cardContainer.setClickable(enabled);
    }

    public boolean isScoreClickEnabled() {
        return scoreContainer.isClickable() && cardContainer.isClickable();
    }

    public
    @IdRes
    int getCardViewId() {
        return cardContainer.getId();
    }

    public void setBackgroundSolid(final boolean backgroundSolid,
                                   final int duration) {
        final int targetAlpha = backgroundSolid ? 0 : 255;
        if (targetAlpha == gradientBackgroundAlpha) {
            return;
        }

        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }

        if (duration == 0) {
            gradientBackground.setAlpha(targetAlpha);
            this.gradientBackgroundAlpha = targetAlpha;
            invalidate();
        } else {
            this.backgroundAnimator = ValueAnimator.ofInt(255 - targetAlpha, targetAlpha);
            AnimatorTemplate.DEFAULT.apply(backgroundAnimator);
            backgroundAnimator.setDuration(500L);
            backgroundAnimator.addUpdateListener(a -> {
                final int alpha = (int) a.getAnimatedValue();
                gradientBackground.setAlpha(alpha);
                this.gradientBackgroundAlpha = alpha;
                invalidate();
            });
            backgroundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    if (TimelineHeaderView.this.backgroundAnimator == animation) {
                        gradientBackground.setAlpha(targetAlpha);
                        TimelineHeaderView.this.gradientBackgroundAlpha = targetAlpha;
                        invalidate();
                        TimelineHeaderView.this.backgroundAnimator = null;
                    }
                }
            });
            backgroundAnimator.start();
        }
    }

    //endregion


    //region Animation

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }

        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }

        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }

        Anime.cancelAll(scoreContainer, cardContainer);
    }

    public void startPulsing() {
        if (pulseAnimator != null || !animationEnabled) {
            return;
        }

        this.pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        final int startColor = getResources().getColor(R.color.timeline_header);
        final int endColor = getResources().getColor(R.color.border);
        pulseAnimator.addUpdateListener(a -> {
            final int color = Anime.interpolateColors(a.getAnimatedFraction(), endColor, startColor);
            scoreDrawable.setTrackColor(color);
        });
        pulseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                scoreDrawable.setTrackColor(endColor);
                TimelineHeaderView.this.pulseAnimator = null;
            }
        });

        scoreDrawable.setValue(0);

        animatorContext.bind(pulseAnimator, PULSE_ANIMATOR_NAME);
        pulseAnimator.start();
    }

    public void stopPulsing() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }

    //endregion


    //region Scores

    private void setScore(@Nullable final Integer score,
                          final ScoreCondition condition) {
        clearAnimation();

        int color;
        if (score == null || condition == ScoreCondition.UNAVAILABLE) {
            color = getResources().getColor(ScoreCondition.UNAVAILABLE.colorRes);

            scoreDrawable.setValue(0);
            scoreText.setText(R.string.missing_data_placeholder);
            scoreContainer.setContentDescription(getResources().getString(R.string.accessibility_sleep_score_unknown));

            setWillNotDraw(true);
        } else {
            color = getResources().getColor(condition.colorRes);

            scoreDrawable.setValue(score);
            scoreText.setText(Integer.toString(score));
            scoreContainer.setContentDescription(getResources().getString(R.string.accessibility_sleep_score_fmt, score));

            setWillNotDraw(false);
        }

        scoreDrawable.setFillColor(color);
        scoreText.setTextColor(color);

        scoreContainer.setTranslationY(0f);
        cardContainer.setVisibility(VISIBLE);

        setBackgroundSolid(false, 0);
    }

    private void animateToScore(@Nullable final Integer score,
                                final ScoreCondition condition,
                                @NonNull final Runnable fireBackgroundAnimations,
                                @NonNull final Runnable fireAdapterAnimations) {
        if (score == null || !animationEnabled || hasAnimated || getVisibility() != VISIBLE) {
            setScore(score, condition);
            fireBackgroundAnimations.run();
            fireAdapterAnimations.run();
        } else {
            stopPulsing();

            setWillNotDraw(false);

            if (scoreAnimator != null) {
                scoreAnimator.cancel();
            }

            this.hasAnimated = true;

            this.scoreAnimator = ValueAnimator.ofInt(scoreDrawable.getValue(), score);
            scoreAnimator.setDuration(Anime.DURATION_SLOW);
            scoreAnimator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);

            final int startColor = scoreDrawable.getFillColor();
            final int endColor = getResources().getColor(condition.colorRes);
            scoreAnimator.addUpdateListener(a -> {
                final Integer newScore = (Integer) a.getAnimatedValue();
                final int color = Anime.interpolateColors(a.getAnimatedFraction(), startColor, endColor);

                scoreDrawable.setValue(newScore);
                scoreDrawable.setFillColor(color);

                scoreText.setText(newScore.toString());
                scoreText.setTextColor(color);
            });
            scoreAnimator.addListener(new AnimatorListenerAdapter() {
                boolean wasCanceled = false;

                @Override
                public void onAnimationCancel(final Animator animation) {
                    scoreDrawable.setValue(score);
                    scoreDrawable.setFillColor(endColor);

                    scoreText.setText(Integer.toString(score));
                    scoreText.setTextColor(endColor);

                    this.wasCanceled = true;
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if (scoreAnimator == animation) {
                        TimelineHeaderView.this.scoreAnimator = null;

                        if (!wasCanceled) {
                            animateScoreIntoPlace(fireBackgroundAnimations, fireAdapterAnimations);
                        }
                    }
                }
            });

            scoreContainer.setContentDescription(getResources().getString(R.string.accessibility_sleep_score_fmt, score));

            animatorContext.bind(scoreAnimator, SCORE_ANIMATOR_NAME);
            animatorContext.startWhenIdle(scoreAnimator);
        }
    }

    private void animateScoreIntoPlace(@NonNull final Runnable fireBackgroundAnimations,
                                       @NonNull final Runnable fireAdapterAnimations) {
        animatorFor(scoreContainer, animatorContext)
                .withInterpolator(new FastOutSlowInInterpolator())
                .translationY(0f)
                .addOnAnimationCompleted(finished -> {
                    if (!finished) {
                        scoreContainer.setTranslationY(0f);
                    }

                    fireAdapterAnimations.run();

                    if (finished) {
                        animateCardIntoView();
                    } else {
                        cardContainer.setAlpha(1f);
                        cardContainer.setVisibility(VISIBLE);
                    }
                })
                .start();

        fireBackgroundAnimations.run();
        setBackgroundSolid(false, Anime.DURATION_NORMAL);
    }

    private void animateCardIntoView() {
        // Intentionally not included in the animator context so
        // that the `TimelineFadeItemAnimator` runs when intended.
        animatorFor(cardContainer)
                .fadeIn()
                .addOnAnimationCompleted(finished -> {
                    if (!finished) {
                        cardContainer.setVisibility(VISIBLE);
                    }
                })
                .start();
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull final Timeline timeline,
                             @NonNull final Runnable fireBackgroundAnimations,
                             @NonNull final Runnable fireAdapterAnimations) {
        cardContents.setText(timeline.getMessage());
        animateToScore(timeline.getScore(),
                       timeline.getScoreCondition(),
                       fireBackgroundAnimations,
                       fireAdapterAnimations);
    }

    public void bindTimeline(@NonNull final Timeline timeline) {
        cardContents.setText(timeline.getMessage());
        setScore(timeline.getScore(),
                 timeline.getScoreCondition());
    }

    //endregion
}
