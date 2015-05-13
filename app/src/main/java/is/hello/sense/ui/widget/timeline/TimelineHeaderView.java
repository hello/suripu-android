package is.hello.sense.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Markdown;

public class TimelineHeaderView extends LinearLayout {
    @Inject Markdown markdown;

    private final SleepScoreDrawable scoreDrawable;
    private final TextView scoreText;
    private final TextView messageText;

    private @Nullable ValueAnimator colorAnimator;
    private @Nullable AnimatorContext animatorContext;


    //region Lifecycle

    public TimelineHeaderView(@NonNull Context context) {
        this(context, null);
    }

    public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        SenseApplication.getInstance().inject(this);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_header, this, true);

        View scoreContainer = findViewById(R.id.view_timeline_header_chart);
        this.scoreDrawable = new SleepScoreDrawable(getResources());
        scoreContainer.setBackground(scoreDrawable);

        this.scoreText = (TextView) findViewById(R.id.view_timeline_header_chart_score);
        this.messageText = (TextView) findViewById(R.id.view_timeline_header_chart_message);
        Views.makeTextViewLinksClickable(messageText);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE && colorAnimator != null) {
            colorAnimator.cancel();
        }
    }

    //endregion


    //region Attributes

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    //endregion


    //region Scores

    public void showScore(int score) {
        if (score < 0) {
            scoreDrawable.setFillColor(getResources().getColor(R.color.sensor_unknown));
            scoreDrawable.setValue(0);
            scoreText.setText(R.string.missing_data_placeholder);
        } else {
            animateToScore(score);
        }
    }

    public void animateToScore(int score) {
        if (score < 0) {
            showScore(score);
        } else {
            if (colorAnimator != null) {
                colorAnimator.cancel();
            }

            this.colorAnimator = ValueAnimator.ofInt(scoreDrawable.getValue(), score);
            colorAnimator.setStartDelay(250);
            colorAnimator.setDuration(Animation.DURATION_NORMAL);
            colorAnimator.setInterpolator(Animation.INTERPOLATOR_DEFAULT);

            ArgbEvaluator colorEvaluator = new ArgbEvaluator();
            int startColor = Styles.getSleepScoreColor(getContext(), scoreDrawable.getValue());
            int endColor = Styles.getSleepScoreColor(getContext(), score);
            colorAnimator.addUpdateListener(a -> {
                Integer newScore = (Integer) a.getAnimatedValue();
                int color = (int) colorEvaluator.evaluate(a.getAnimatedFraction(), startColor, endColor);

                scoreDrawable.setValue(newScore);
                scoreDrawable.setFillColor(color);

                scoreText.setText(newScore.toString());
                scoreText.setTextColor(color);
            });
            colorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    scoreDrawable.setValue(score);
                    scoreDrawable.setFillColor(endColor);

                    scoreText.setText(Integer.toString(score));
                    scoreText.setTextColor(endColor);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (colorAnimator == animation) {
                        TimelineHeaderView.this.colorAnimator = null;
                    }
                }
            });

            if (animatorContext != null) {
                colorAnimator.addListener(animatorContext);
                animatorContext.runWhenIdle(colorAnimator::start);
            } else {
                colorAnimator.start();
            }
        }
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull Timeline timeline) {
        if (Lists.isEmpty(timeline.getSegments())) {
            showScore(-1);
        } else {
            int sleepScore = timeline.getScore();
            animateToScore(sleepScore);
        }

        markdown.renderInto(messageText, timeline.getMessage());
    }

    public void timelineUnavailable(@NonNull Throwable e) {
        showScore(-1);
        messageText.setText(getResources().getString(R.string.timeline_error_message, e.getMessage()));
    }

    //endregion
}
