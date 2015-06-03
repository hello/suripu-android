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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.ConditionSummary;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class TimelineHeaderView extends RelativeLayout implements TimelineFadeItemAnimator.Listener {
    public static final int NULL_SCORE = -1;


    private final Paint dividerPaint = new Paint();
    private final int dividerHeight;


    private final View topFadeView;
    private final View scoreContainer;
    private final SleepScoreDrawable scoreDrawable;
    private final TextView scoreText;
    private final TextView messageText;

    private final ViewGroup card;
    private final TextView cardTitle;
    private final TextView cardContents;
    private final TextView cardCallToAction;

    private final int backgroundColor;
    private final int messageTextColor;

    private boolean hasAnimated = false;
    private boolean animationEnabled = true;
    private AnimatorContext animatorContext;


    private @Nullable ValueAnimator colorAnimator;
    private @Nullable ValueAnimator pulseAnimator;


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
        this.backgroundColor = resources.getColor(R.color.background_timeline);

        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
            backgroundColor,
            backgroundColor,
            resources.getColor(R.color.timeline_header_gradient_end),
        });
        setBackground(gradient);


        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_header, this, true);

        this.topFadeView = findViewById(R.id.view_timeline_header_fade);

        this.scoreContainer = findViewById(R.id.view_timeline_header_chart);
        int scoreTranslation = resources.getDimensionPixelSize(R.dimen.grand_sleep_summary_height) / 2;
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
        this.messageText = (TextView) findViewById(R.id.view_timeline_header_chart_message);
        messageText.setAlpha(0f);
        Views.makeTextViewLinksClickable(messageText);

        this.messageTextColor = messageText.getCurrentTextColor();


        this.card = (ViewGroup) findViewById(R.id.view_timeline_header_card);
        card.setAlpha(0f);

        this.cardTitle = (TextView) card.findViewById(R.id.view_timeline_header_card_title);
        this.cardContents = (TextView) card.findViewById(R.id.view_timeline_header_card_contents);
        this.cardCallToAction = (TextView) card.findViewById(R.id.view_timeline_header_card_cta);
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

        canvas.drawRect(0, bottom - dividerHeight,
                right, bottom, dividerPaint);
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
            Views.setSafeOnClickListener(scoreContainer, listener);
        } else {
            scoreContainer.setOnClickListener(null);
        }
    }

    public void setChildFadeAmount(float amount) {
        scoreDrawable.setAlpha(Math.round(255f * amount));
        scoreText.setAlpha(amount);

        // messageText has rich text, setAlpha is too expensive.
        messageText.setTextColor(Drawing.interpolateColors(amount, backgroundColor, messageTextColor));

        topFadeView.setTranslationY(-getTop());
    }

    private void setCardTitleTint(int color) {
        cardCallToAction.setTextColor(color);
        Drawables.setTintColor(cardCallToAction.getCompoundDrawablesRelative()[2].mutate(), color);
        Drawables.setTintColor(cardTitle.getCompoundDrawablesRelative()[0].mutate(), color);
    }

    //endregion


    //region Animation

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        PropertyAnimatorProxy.stop(scoreContainer, messageText);
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
            int color = Drawing.interpolateColors(a.getAnimatedFraction(), endColor, startColor);
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

    private void setScore(int score) {
        clearAnimation();

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

        scoreContainer.setTranslationY(0f);
        messageText.setAlpha(1f);
        card.setAlpha(1f);
    }

    private void animateToScore(int score, @NonNull Runnable fireAdapterAnimations) {
        if (score < 0 || !animationEnabled || hasAnimated || getVisibility() != VISIBLE) {
            setScore(score);
            fireAdapterAnimations.run();
        } else {
            stopPulsing();

            setWillNotDraw(false);

            if (colorAnimator != null) {
                colorAnimator.cancel();
            }

            this.hasAnimated = true;

            this.colorAnimator = ValueAnimator.ofInt(scoreDrawable.getValue(), score);
            colorAnimator.setDuration(Animation.DURATION_NORMAL);
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

                    this.wasCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (colorAnimator == animation) {
                        TimelineHeaderView.this.colorAnimator = null;

                        if (!wasCanceled) {
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
             .setInterpolator(new AccelerateDecelerateInterpolator())
             .translationY(0f);

            f.animate(messageText)
             .setStartDelay(Animation.DURATION_NORMAL / 2)
             .fadeIn();

            f.animate(card)
             .setStartDelay(Animation.DURATION_NORMAL)
             .fadeIn();
        }, finished -> {
            if (!finished) {
                scoreContainer.setTranslationY(0f);
                messageText.setAlpha(1f);
                card.setAlpha(1f);
            }

            fireAdapterAnimations.run();
        });
    }

    //endregion


    //region Binding

    public void bindMessage(@Nullable CharSequence message) {
        messageText.setText(message);
    }

    public void bindConditionSummary(@NonNull ConditionSummary summary) {
        Resources resources = getResources();
        int averageColor = resources.getColor(summary.getAverageCondition().colorRes);

        cardTitle.setText(R.string.title_bedroom_environment);
        setCardTitleTint(averageColor);

        //noinspection ConstantConditions
        Drawable sensorIcon = resources.getDrawable(summary.getBadSensor().iconRes).mutate();
        int badSensorColor = resources.getColor(summary.getBadSensorCondition().colorRes);
        Drawables.setTintColor(sensorIcon, badSensorColor);
        cardContents.setCompoundDrawablesRelativeWithIntrinsicBounds(sensorIcon, null, null, null);
        cardContents.setText(summary.getMessage());
    }

    public void bindScore(int score, @NonNull Runnable fireAdapterAnimations) {
        animateToScore(score, fireAdapterAnimations);
    }

    public void bindError(@NonNull Throwable e) {
        messageText.setText(R.string.missing_data_placeholder);

        cardTitle.setText(R.string.dialog_error_title);
        cardCallToAction.setVisibility(INVISIBLE);
        setCardTitleTint(getResources().getColor(R.color.sensor_unknown));

        cardContents.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
        cardContents.setText(getResources().getString(R.string.timeline_error_message, e.getMessage()));

        setScore(NULL_SCORE);
    }

    //endregion


    //region Timeline Animations

    @Override
    public void onTimelineAnimationWillStart(@NonNull AnimatorContext animatorContext, @NonNull AnimatorContext.TransactionFacade f) {
        if (messageText.getAlpha() > 0f) {
            return;
        }

        f.animate(messageText)
         .fadeIn();
    }

    @Override
    public void onTimelineAnimationDidEnd(boolean finished) {
        if (!finished) {
            PropertyAnimatorProxy.stop(messageText);
            messageText.setAlpha(1f);
        }
    }

    //endregion
}
