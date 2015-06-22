package is.hello.sense.ui.widget.timeline;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.functional.Functions;
import is.hello.sense.ui.adapter.TimelineAdapter;
import is.hello.sense.ui.animation.AnimatorContext;

public abstract class AbstractTimelineItemAnimator extends RecyclerView.ItemAnimator {
    private final AnimatorContext animatorContext;
    private final List<Listener> listeners = new ArrayList<>();

    protected boolean enabled = true;

    protected AbstractTimelineItemAnimator(@NonNull AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
        setSupportsChangeAnimations(false);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AnimatorContext getAnimatorContext() {
        return animatorContext;
    }

    protected static void sortByPosition(@NonNull List<? extends RecyclerView.ViewHolder> viewHolders) {
        Collections.sort(viewHolders, (l, r) -> Functions.compareInts(l.getLayoutPosition(), r.getLayoutPosition()));
    }

    protected boolean isViewHolderAnimated(@NonNull RecyclerView.ViewHolder viewHolder) {
        return (enabled && viewHolder.getAdapterPosition() >= TimelineAdapter.STATIC_ITEM_COUNT);
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

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    protected void dispatchAnimationWillStart(@NonNull AnimatorContext.TransactionFacade transactionFacade) {
        for (Listener listener : listeners) {
            listener.onTimelineAnimationWillStart(animatorContext, transactionFacade);
        }
    }

    protected void dispatchAnimationDidEnd(boolean finished) {
        dispatchAnimationsFinished();
        for (Listener listener : listeners) {
            listener.onTimelineAnimationDidEnd(finished);
        }
    }

    public interface Listener {
        void onTimelineAnimationWillStart(@NonNull AnimatorContext animatorContext, @NonNull AnimatorContext.TransactionFacade transactionFacade);
        void onTimelineAnimationDidEnd(boolean finished);
    }

    //endregion
}
