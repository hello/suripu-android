package is.hello.sense.ui.widget.graphing.trends;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;
import is.hello.sense.util.NotTested;

@NotTested
public abstract class TrendGraphView extends View implements TrendFeedViewItem.OnBindGraph {
    protected TrendGraphDrawable drawable;
    protected AnimatorContext animatorContext;
    protected static final float maxAnimationFactor = 1f;
    protected static final float minAnimationFactor = 0;
    protected boolean isAnimating = false;
    @Nullable
    protected AnimationCallback animationCallback;

    protected TrendGraphView(@NonNull final Context context,
                             @NonNull final AnimatorContext animatorContext,
                             @NonNull final AnimationCallback animationCallback) {
        super(context);
        this.animatorContext = animatorContext;
        this.animationCallback = animationCallback;

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animationCallback != null) {
            animationCallback = null;
        }
    }

    public Graph getGraph() {
        return drawable.getGraph();
    }

    @Override
    public void bindGraph(@NonNull final Graph graph) {
        drawable.updateGraph(graph);
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    protected void finishedAnimating() {
        isAnimating = false;
        if (animationCallback != null) {
            animationCallback.isFinished();
        }
    }

    public void fadeOut(@Nullable final Animator.AnimatorListener animatorListener) {
        fade(ValueAnimator.ofFloat(maxAnimationFactor, minAnimationFactor), animatorListener);
    }

    public void fadeIn(@Nullable final Animator.AnimatorListener animatorListener) {
        fade(ValueAnimator.ofFloat(minAnimationFactor, maxAnimationFactor), animatorListener);
    }

    private void fade(@NonNull final ValueAnimator animator, @Nullable final Animator.AnimatorListener animatorListener) {
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