package is.hello.sense.ui.widget.graphing;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

@SuppressLint("ViewConstructor")
public abstract class TrendGraphView extends View implements TrendFeedViewItem.OnBindGraph {
    protected TrendGraphDrawable drawable;
    protected AnimatorContext animatorContext;
    protected static final float maxAnimationFactor = 1f;
    protected static final float minAnimationFactor = 0;
    protected boolean isAnimating = false;
    protected AnimationCallback animationCallback;

    protected TrendGraphView(@NonNull Context context, @NonNull AnimatorContext animatorContext, @NonNull AnimationCallback animationCallback) {
        super(context);
        this.animatorContext = animatorContext;
        this.animationCallback = animationCallback;

    }

    public Graph getGraph() {
        return drawable.getGraph();
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    protected void finishedAnimating() {
        isAnimating = false;
        animationCallback.isFinished();
    }

    public void fadeOut(@Nullable Animator.AnimatorListener animatorListener) {
        fade(ValueAnimator.ofFloat(maxAnimationFactor, minAnimationFactor), animatorListener);
    }

    public void fadeIn(@Nullable Animator.AnimatorListener animatorListener) {
        fade(ValueAnimator.ofFloat(minAnimationFactor, maxAnimationFactor), animatorListener);
    }

    private void fade(@NonNull ValueAnimator animator, @Nullable Animator.AnimatorListener animatorListener) {
        animator.setDuration(Anime.DURATION_NORMAL);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(a -> {
            setAlpha((float) a.getAnimatedValue());
        });
        if (animatorListener != null) {
            animator.addListener(animatorListener);
        }
        animatorContext.startWhenIdle(animator);
    }

    public interface AnimationCallback {
        void isFinished();
    }

}