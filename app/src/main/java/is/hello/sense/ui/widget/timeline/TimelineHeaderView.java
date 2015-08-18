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
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
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
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.util.SafeOnClickListener;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class TimelineHeaderView extends RelativeLayout {
    private final Paint paint = new Paint();
    private final int dividerHeight;
    private final int dividerColor;

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


    private @Nullable ValueAnimator scoreAnimator;
    private @Nullable ValueAnimator pulseAnimator;
    private @Nullable ValueAnimator backgroundAnimator;


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
        this.dividerColor = resources.getColor(R.color.timeline_header_border);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);

        this.solidBackgroundColor = resources.getColor(R.color.background_timeline);
        this.gradientBackground = ResourcesCompat.getDrawable(resources, R.drawable.background_timeline_header, null);
        gradientBackground.setAlpha(0);


        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_header, this, true);

        this.scoreContainer = findViewById(R.id.view_timeline_header_chart);
        int scoreTranslation = resources.getDimensionPixelSize(R.dimen.gap_xlarge);
        scoreContainer.setTranslationY(scoreTranslation);

        this.scoreDrawable = new SleepScoreDrawable(getResources(), true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int rippleColor = scoreDrawable.getPressedColor();
            ShapeDrawable mask = new ShapeDrawable(new OvalShape());
            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(rippleColor), scoreDrawable, mask);
            scoreContainer.setBackground(ripple);
        } else {
            scoreDrawable.setStateful(true);
            scoreContainer.setBackground(scoreDrawable);
        }

        this.scoreText = (TextView) findViewById(R.id.view_timeline_header_chart_score);


        this.cardContainer = (ViewGroup) findViewById(R.id.view_timeline_header_card);
        cardContainer.setVisibility(INVISIBLE);

        TextView cardTitle = (TextView) cardContainer.findViewById(R.id.view_timeline_header_card_title);
        Drawable end = cardTitle.getCompoundDrawablesRelative()[2];
        Drawables.setTintColor(end, resources.getColor(R.color.light_accent));
        this.cardContents = (TextView) cardContainer.findViewById(R.id.view_timeline_header_card_contents);


        setScoreClickEnabled(false);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int right = canvas.getWidth(),
            bottom = canvas.getHeight();

        if (gradientBackgroundAlpha < 255) {
            paint.setColor(solidBackgroundColor);
            canvas.drawRect(0, 0, right, bottom, paint);
        }

        if (gradientBackgroundAlpha > 0) {
            gradientBackground.setBounds(0, 0, right, bottom);
            gradientBackground.draw(canvas);

            paint.setColor(Drawing.colorWithAlpha(dividerColor, gradientBackgroundAlpha));
            canvas.drawRect(0, bottom - dividerHeight,
                            right, bottom, paint);
        }
    }

    //endregion


    //region Attributes

    public void setAnimationEnabled(boolean animationEnabled) {
        if (!animationEnabled) {
            clearAnimation();
        }

        this.animationEnabled = animationEnabled;
    }

    public void setAnimatorContext(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setOnScoreClickListener(@Nullable View.OnClickListener listener) {
        if (listener != null) {
            SafeOnClickListener wrapper = new SafeOnClickListener(listener);
            boolean clickEnabled = isScoreClickEnabled();
            scoreContainer.setOnClickListener(wrapper);
            cardContainer.setOnClickListener(wrapper);
            setScoreClickEnabled(clickEnabled);
        } else {
            scoreContainer.setOnClickListener(null);
            cardContainer.setOnClickListener(null);
            setScoreClickEnabled(false);
        }
    }

    public void setScoreClickEnabled(boolean enabled) {
        scoreContainer.setClickable(enabled);
        cardContainer.setClickable(enabled);
    }

    public boolean isScoreClickEnabled() {
        return scoreContainer.isClickable() && cardContainer.isClickable();
    }

    public @IdRes int getCardViewId() {
        return cardContainer.getId();
    }

    public void setBackgroundSolid(boolean backgroundSolid, int duration) {
        int targetAlpha = backgroundSolid ? 0 : 255;
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
            ValueAnimator alphaAnimator = ValueAnimator.ofInt(255 - targetAlpha, targetAlpha);
            AnimatorTemplate.DEFAULT.apply(alphaAnimator);
            alphaAnimator.addUpdateListener(a -> {
                int alpha = (int) a.getAnimatedValue();
                gradientBackground.setAlpha(alpha);
                this.gradientBackgroundAlpha = alpha;
                invalidate();
            });
            alphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (backgroundAnimator == animation) {
                        gradientBackground.setAlpha(targetAlpha);
                        TimelineHeaderView.this.gradientBackgroundAlpha = targetAlpha;
                        invalidate();
                        TimelineHeaderView.this.backgroundAnimator = null;
                    }
                }
            });
            alphaAnimator.start();
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

        int startColor = getResources().getColor(R.color.light_accent);
        int endColor = getResources().getColor(R.color.border);
        pulseAnimator.addUpdateListener(a -> {
            int color = Anime.interpolateColors(a.getAnimatedFraction(), endColor, startColor);
            scoreDrawable.setTrackColor(color);
        });
        pulseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scoreDrawable.setTrackColor(endColor);
                TimelineHeaderView.this.pulseAnimator = null;
            }
        });

        scoreDrawable.setValue(0);

        pulseAnimator.addListener(animatorContext);
        pulseAnimator.start();
    }

    public void stopPulsing() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }

    //endregion


    //region Scores

    private void setScore(@Nullable Integer score, ScoreCondition condition) {
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

    private void animateToScore(@Nullable Integer score,
                                ScoreCondition condition,
                                @NonNull Runnable fireBackgroundAnimations,
                                @NonNull Runnable fireAdapterAnimations) {
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

            int startColor = scoreDrawable.getFillColor();
            int endColor = getResources().getColor(condition.colorRes);
            scoreAnimator.addUpdateListener(a -> {
                Integer newScore = (Integer) a.getAnimatedValue();
                int color = Anime.interpolateColors(a.getAnimatedFraction(), startColor, endColor);

                scoreDrawable.setValue(newScore);
                scoreDrawable.setFillColor(color);

                scoreText.setText(newScore.toString());
                scoreText.setTextColor(color);
            });
            scoreAnimator.addListener(new AnimatorListenerAdapter() {
                boolean wasCanceled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    scoreDrawable.setValue(score);
                    scoreDrawable.setFillColor(endColor);

                    scoreText.setText(Integer.toString(score));
                    scoreText.setTextColor(endColor);

                    this.wasCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (scoreAnimator == animation) {
                        TimelineHeaderView.this.scoreAnimator = null;

                        if (!wasCanceled) {
                            animateScoreIntoPlace(fireBackgroundAnimations, fireAdapterAnimations);
                        }
                    }
                }
            });

            scoreContainer.setContentDescription(getResources().getString(R.string.accessibility_sleep_score_fmt, score));

            scoreAnimator.addListener(animatorContext);
            animatorContext.startWhenIdle(scoreAnimator);
        }
    }

    private void animateScoreIntoPlace(@NonNull Runnable fireBackgroundAnimations,
                                       @NonNull Runnable fireAdapterAnimations) {
        animatorFor(scoreContainer, animatorContext)
                .withInterpolator(new FastOutSlowInInterpolator())
                .translationY(0f)
                .addOnAnimationCompleted(finished -> {
                    if (!finished) {
                        scoreContainer.setTranslationY(0f);
                    }

                    fireAdapterAnimations.run();
                    animateCardIntoView();
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

    public void bindTimeline(@NonNull Timeline timeline,
                             @NonNull Runnable fireBackgroundAnimations,
                             @NonNull Runnable fireAdapterAnimations) {
        cardContents.setText(timeline.getMessage());
        animateToScore(timeline.getScore(),
                       timeline.getScoreCondition(),
                       fireBackgroundAnimations,
                       fireAdapterAnimations);
    }

    //endregion
}
