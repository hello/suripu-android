package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;

public abstract class AbstractTimelineItemAnimator extends RecyclerView.ItemAnimator {
    private final AnimatorContext animatorContext;
    private final Listener listener;

    protected AbstractTimelineItemAnimator(@NonNull AnimatorContext animatorContext,
                                           @NonNull Listener listener) {
        this.animatorContext = animatorContext;
        this.listener = listener;
    }

    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }


    //region Default Implementations

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        dispatchAddFinished(holder);
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
                                 RecyclerView.ViewHolder newHolder,
                                 int fromLeft,
                                 int fromTop,
                                 int toLeft,
                                 int toTop) {
        dispatchChangeFinished(oldHolder, true);
        return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        dispatchMoveFinished(holder);
        return false;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        dispatchRemoveFinished(holder);
        return false;
    }

    //endregion


    //region Listener

    protected void dispatchAnimationWillStart(@NonNull AnimatorConfig animatorConfig) {
        listener.onTimelineAnimationWillStart(animatorContext, animatorConfig);
    }

    protected void dispatchAnimationDidEnd(boolean finished) {
        listener.onTimelineAnimationDidEnd(finished);
    }

    public interface Listener {
        void onTimelineAnimationWillStart(@NonNull AnimatorContext animatorContext, @NonNull AnimatorConfig animatorConfig);
        void onTimelineAnimationDidEnd(boolean finished);
    }

    //endregion
}
